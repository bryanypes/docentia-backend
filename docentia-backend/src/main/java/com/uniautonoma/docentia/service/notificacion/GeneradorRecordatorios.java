package com.uniautonoma.docentia.service.notificacion;

import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.EstadoCita;
import com.uniautonoma.docentia.entity.EstadoFormulario;
import com.uniautonoma.docentia.entity.EtapaPeriodo;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.TipoFormulario;
import com.uniautonoma.docentia.entity.TipoNotificacion;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CitaRepository;
import com.uniautonoma.docentia.repository.CompromisoRepository;
import com.uniautonoma.docentia.repository.EvaluacionFormularioRepository;
import com.uniautonoma.docentia.repository.EvaluacionFormularioRepository.AvanceFormulario;
import com.uniautonoma.docentia.repository.FacultadRepository;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.service.heteroevaluacion.CoberturaService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
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
  private final EvaluacionFormularioRepository formularioRepository;
  private final JdbcTemplate jdbcTemplate;
  private final CoberturaService cobertura;
  private final Clock reloj;

  public GeneradorRecordatorios(
      NotificacionService notificaciones,
      CitaRepository citaRepository,
      CompromisoRepository compromisoRepository,
      PeriodoAcademicoRepository periodoRepository,
      UsuarioRepository usuarioRepository,
      FacultadRepository facultadRepository,
      EvaluacionFormularioRepository formularioRepository,
      JdbcTemplate jdbcTemplate,
      CoberturaService cobertura,
      Clock reloj) {
    this.jdbcTemplate = jdbcTemplate;
    this.cobertura = cobertura;
    this.formularioRepository = formularioRepository;
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
      if (etapa.getTipoEtapa() == TipoEtapa.RECOLECCION_CALIFICACIONES) {
        recordarFormularios(periodo, etapa, hoy);
        recordarHeteroevaluacion(periodo, etapa, hoy);
      }
      if (!hoy.isBefore(etapa.getFechaCierre().minusDays(3))) {
        avisarAFacultades(TipoNotificacion.VENTANA_POR_CERRAR, etapa, clave, idPeriodo);
      }
    }
  }

  /** A 3 días y a 1 día del cierre, a quien aún no envía su formulario. */
  private void recordarFormularios(PeriodoAcademico periodo, EtapaPeriodo etapa, LocalDate hoy) {
    long dias = ChronoUnit.DAYS.between(hoy, etapa.getFechaCierre());
    if (dias > 3) {
      return;
    }
    String plazo = dias <= 1 ? "1d" : "3d";
    Long idPeriodo = periodo.getIdPeriodo();
    List<AvanceFormulario> avance = formularioRepository.leerAvance(idPeriodo);
    Set<Long> conAutoevaluacion = enviadas(avance, TipoFormulario.AUTO);
    Set<Long> conCoevaluacion = enviadas(avance, TipoFormulario.COEVAL);
    Map<Long, Long> coevaluacionesPorFacultad = new HashMap<>();
    for (Usuario docente :
        usuarioRepository.findByRolAndEsActivoTrueOrderByNombreCompletoAsc(Rol.DOCENTE)) {
      if (!conAutoevaluacion.contains(docente.getIdUsuario())) {
        notificaciones.notificar(
            EventoNotificacion.aUsuario(
                TipoNotificacion.AUTOEVALUACION_PENDIENTE,
                docente.getIdUsuario(),
                "/mi-autoevaluacion",
                "PERIODO",
                idPeriodo,
                "autoevaluacion:" + idPeriodo + ":" + plazo));
      }
      if (!conCoevaluacion.contains(docente.getIdUsuario())) {
        coevaluacionesPorFacultad.merge(
            docente.getPrograma().getFacultad().getIdFacultad(), 1L, Long::sum);
      }
    }
    coevaluacionesPorFacultad.forEach(
        (idFacultad, cantidad) ->
            notificaciones.notificar(
                new EventoNotificacion(
                    TipoNotificacion.COEVALUACIONES_PENDIENTES,
                    EventoNotificacion.Destino.COORDINADORES_FACULTAD,
                    idFacultad,
                    "/coevaluaciones",
                    "PERIODO",
                    idPeriodo,
                    "coevaluacion:" + idPeriodo + ":" + plazo,
                    "Pendientes: " + cantidad + ".")));
  }

  /** Estudiantes con evaluaciones pendientes: al abrir, y a 3 días y a 1 día del cierre. */
  private void recordarHeteroevaluacion(
      PeriodoAcademico periodo, EtapaPeriodo etapa, LocalDate hoy) {
    long dias = ChronoUnit.DAYS.between(hoy, etapa.getFechaCierre());
    boolean apertura = !hoy.isAfter(etapa.getFechaApertura().plusDays(DIAS_AVISO_APERTURA));
    if (!apertura && dias > 3) {
      return;
    }
    String momento = dias <= 1 ? "1d" : dias <= 3 ? "3d" : "apertura";
    Long idPeriodo = periodo.getIdPeriodo();
    jdbcTemplate
        .queryForList(
            """
            select distinct m.estudiante_id
            from matriculas m
            join clases c on c.id = m.clase_id and c.id_periodo = ?
            join clases_docente cd on cd.clase_id = c.id
            where m.es_activa and not exists (
                select 1 from participaciones_heteroevaluacion pa
                where pa.asignacion_id = cd.id and pa.estudiante_id = m.estudiante_id)
            """,
            Long.class,
            idPeriodo)
        .forEach(
            idEstudiante ->
                notificaciones.notificar(
                    EventoNotificacion.aUsuario(
                        TipoNotificacion.HETEROEVALUACION_PENDIENTE,
                        idEstudiante,
                        "/evaluacion-profesores",
                        "PERIODO",
                        idPeriodo,
                        "heteroevaluacion:" + idPeriodo + ":" + momento)));
    if (dias <= 3 && dias > 1) {
      facultadRepository
          .findAll()
          .forEach(
              facultad -> {
                if (cobertura.tieneMatriculados(idPeriodo, facultad.getIdFacultad())
                    && cobertura
                            .porcentajeFacultad(idPeriodo, facultad.getIdFacultad())
                            .compareTo(java.math.BigDecimal.valueOf(50))
                        < 0) {
                  notificaciones.notificar(
                      new EventoNotificacion(
                          TipoNotificacion.COBERTURA_BAJA,
                          EventoNotificacion.Destino.COORDINADORES_FACULTAD,
                          facultad.getIdFacultad(),
                          "/matricula",
                          "PERIODO",
                          idPeriodo,
                          "cobertura:" + idPeriodo,
                          null));
                }
              });
    }
  }

  private static Set<Long> enviadas(List<AvanceFormulario> avance, TipoFormulario tipo) {
    return avance.stream()
        .filter(fila -> fila.tipo() == tipo && fila.estado() == EstadoFormulario.ENVIADA)
        .map(AvanceFormulario::idDocente)
        .collect(Collectors.toSet());
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
