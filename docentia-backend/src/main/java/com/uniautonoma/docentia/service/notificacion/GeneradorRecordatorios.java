package com.uniautonoma.docentia.service.notificacion;

import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.EstadoCita;
import com.uniautonoma.docentia.entity.EtapaPeriodo;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.TipoNotificacion;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CitaRepository;
import com.uniautonoma.docentia.repository.CompromisoRepository;
import com.uniautonoma.docentia.repository.FacultadRepository;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea los avisos que dependen del calendario. Cada aviso lleva una clave por suceso, así que
 * correr esto dos veces o reiniciar la app no duplica nada.
 */
@Component
public class GeneradorRecordatorios {

  private static final String ENLACE_PLAN = "/mi-evaluacion";
  private static final int DIAS_AVISO_APERTURA = 3;
  private static final Set<TipoEtapa> ETAPAS_CON_DOCENTES =
      Set.of(TipoEtapa.RECOLECCION_CALIFICACIONES, TipoEtapa.AGENDA_CITAS, TipoEtapa.FIRMA_ACTAS);

  private final NotificacionService notificaciones;
  private final CitaRepository citaRepository;
  private final CompromisoRepository compromisoRepository;
  private final PeriodoAcademicoRepository periodoRepository;
  private final UsuarioRepository usuarioRepository;
  private final FacultadRepository facultadRepository;
  private final Clock reloj;

  public GeneradorRecordatorios(
      NotificacionService notificaciones,
      CitaRepository citaRepository,
      CompromisoRepository compromisoRepository,
      PeriodoAcademicoRepository periodoRepository,
      UsuarioRepository usuarioRepository,
      FacultadRepository facultadRepository,
      Clock reloj) {
    this.notificaciones = notificaciones;
    this.citaRepository = citaRepository;
    this.compromisoRepository = compromisoRepository;
    this.periodoRepository = periodoRepository;
    this.usuarioRepository = usuarioRepository;
    this.facultadRepository = facultadRepository;
    this.reloj = reloj;
  }

  /** Revisa citas, compromisos y etapas. */
  @Transactional(readOnly = true)
  public void generar() {
    LocalDateTime ahora = LocalDateTime.now(reloj);
    LocalDate hoy = ahora.toLocalDate();
    recordarCitas(ahora);
    recordarCompromisos(hoy);
    periodoRepository.findByEsActivoTrue().ifPresent(periodo -> revisarEtapas(periodo, hoy));
  }

  private void recordarCitas(LocalDateTime ahora) {
    citaRepository
        .buscarConFranjaEntre(
            EstadoCita.ACTIVA, ahora.toLocalDate(), ahora.toLocalDate().plusDays(1))
        .stream()
        .filter(
            cita -> {
              LocalDateTime inicio =
                  cita.getFranja().getFecha().atTime(cita.getFranja().getHoraInicio());
              return inicio.isAfter(ahora) && !inicio.isAfter(ahora.plusHours(24));
            })
        .forEach(
            cita ->
                notificaciones.notificar(
                    EventoNotificacion.aUsuario(
                        TipoNotificacion.RECORDATORIO_CITA,
                        cita.getDocente().getIdUsuario(),
                        "/mi-cita",
                        "CITA",
                        cita.getIdCita(),
                        "cita:" + cita.getIdCita() + ":24h")));
  }

  private void recordarCompromisos(LocalDate hoy) {
    for (Compromiso compromiso : compromisoRepository.buscarVigentesEnFirmeHasta(hoy.plusDays(7))) {
      Long id = compromiso.getIdCompromiso();
      Usuario docente = compromiso.getDocente();
      LocalDate limite = compromiso.getFechaLimite();
      if (limite.isBefore(hoy)) {
        notificaciones.notificar(
            EventoNotificacion.aUsuario(
                TipoNotificacion.COMPROMISO_VENCIDO,
                docente.getIdUsuario(),
                ENLACE_PLAN,
                "COMPROMISO",
                id,
                "compromiso:" + id + ":vencido"));
        notificaciones.notificar(
            EventoNotificacion.aCoordinadores(
                TipoNotificacion.COMPROMISO_VENCIDO_FACULTAD,
                docente,
                "/docentes/" + docente.getIdUsuario(),
                "COMPROMISO",
                id,
                "compromiso:" + id + ":vencido"));
      } else {
        String plazo = limite.isAfter(hoy.plusDays(1)) ? "7d" : "1d";
        notificaciones.notificar(
            EventoNotificacion.aUsuario(
                TipoNotificacion.COMPROMISO_POR_VENCER,
                docente.getIdUsuario(),
                ENLACE_PLAN,
                "COMPROMISO",
                id,
                "compromiso:" + id + ":" + plazo));
      }
    }
  }

  private void revisarEtapas(PeriodoAcademico periodo, LocalDate hoy) {
    for (EtapaPeriodo etapa : periodo.getEtapas()) {
      String clave = "etapa:" + periodo.getIdPeriodo() + ":" + etapa.getTipoEtapa();
      Long idPeriodo = periodo.getIdPeriodo();
      if (hoy.isAfter(etapa.getFechaCierre())) {
        notificaciones.notificar(
            EventoNotificacion.aVicerrectores(
                    TipoNotificacion.VENTANA_CERRADA,
                    "/panel-programas",
                    "PERIODO",
                    idPeriodo,
                    clave)
                .conDetalle(etapa.getTipoEtapa().getNombre() + "."));
        continue;
      }
      if (hoy.isBefore(etapa.getFechaApertura())) {
        continue;
      }
      // Una etapa que ya llevaba días abierta no es noticia, como las del seed.
      if (!hoy.isAfter(etapa.getFechaApertura().plusDays(DIAS_AVISO_APERTURA))) {
        avisarApertura(etapa, clave, idPeriodo);
      }
      if (!hoy.isBefore(etapa.getFechaCierre().minusDays(3))) {
        avisarAFacultades(TipoNotificacion.VENTANA_POR_CERRAR, etapa, clave, idPeriodo);
      }
    }
  }

  private void avisarApertura(EtapaPeriodo etapa, String clave, Long idPeriodo) {
    avisarAFacultades(TipoNotificacion.VENTANA_ABIERTA, etapa, clave, idPeriodo);
    if (ETAPAS_CON_DOCENTES.contains(etapa.getTipoEtapa())) {
      List<Usuario> docentes =
          usuarioRepository.findByRolAndEsActivoTrueOrderByNombreCompletoAsc(Rol.DOCENTE);
      docentes.forEach(
          docente ->
              notificaciones.notificar(
                  EventoNotificacion.aUsuario(
                          TipoNotificacion.VENTANA_ABIERTA,
                          docente.getIdUsuario(),
                          "/mi-evaluacion",
                          "PERIODO",
                          idPeriodo,
                          clave)
                      .conDetalle(etapa.getTipoEtapa().getNombre() + ".")));
    }
  }

  private void avisarAFacultades(
      TipoNotificacion tipo, EtapaPeriodo etapa, String clave, Long idPeriodo) {
    facultadRepository
        .findAll()
        .forEach(
            facultad ->
                notificaciones.notificar(
                    new EventoNotificacion(
                        tipo,
                        EventoNotificacion.Destino.COORDINADORES_FACULTAD,
                        facultad.getIdFacultad(),
                        "/inicio",
                        "PERIODO",
                        idPeriodo,
                        clave,
                        etapa.getTipoEtapa().getNombre() + ".")));
  }
}
