package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.CitaResponse;
import com.uniautonoma.docentia.dto.EstadoPromedio;
import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.ConfirmacionCita;
import com.uniautonoma.docentia.entity.EstadoCita;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoNotificacion;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.CitaRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.notificacion.EventoNotificacion;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Antes de la reunión el docente confirma o declina su cita; en la reunión la coordinación registra
 * si asistió. Asistir le habilita al docente su resultado y la firma del acta.
 */
@Service
@Transactional
public class AsistenciaCitaService {

  private static final String CITA_NO_ENCONTRADA = "Cita no encontrada";
  private static final String ENTIDAD_CITA = "CITA";

  private final CitaRepository citaRepository;
  private final DocenteService docenteService;
  private final PeriodoAcademicoService periodoService;
  private final PromedioService promedioService;
  private final ApplicationEventPublisher eventos;
  private final Clock reloj;

  public AsistenciaCitaService(
      CitaRepository citaRepository,
      DocenteService docenteService,
      PeriodoAcademicoService periodoService,
      PromedioService promedioService,
      ApplicationEventPublisher eventos,
      Clock reloj) {
    this.citaRepository = citaRepository;
    this.docenteService = docenteService;
    this.periodoService = periodoService;
    this.promedioService = promedioService;
    this.eventos = eventos;
    this.reloj = reloj;
  }

  /**
   * El docente confirma o declina su cita activa. Declinar la libera y la franja vuelve a quedar
   * disponible; luego puede reservar otra.
   *
   * @throws RecursoNoEncontradoException si no tiene una cita activa en el periodo
   * @throws ReglaNegocioException si la cita ya empezó, ya registraron su asistencia o repite la
   *     confirmación
   */
  public CitaResponse responderConfirmacion(boolean asiste, UsuarioSesion sesion) {
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    Cita cita =
        citaRepository
            .bloquearActiva(sesion.idUsuario(), periodo.getIdPeriodo())
            .orElseThrow(() -> new RecursoNoEncontradoException("No tienes una cita activa"));
    LocalDateTime ahora = LocalDateTime.now(reloj);
    if (cita.getAsistencia() != null) {
      throw new ReglaNegocioException("La coordinación ya registró tu asistencia");
    }
    if (!inicio(cita).isAfter(ahora)) {
      throw new ReglaNegocioException("La cita ya empezó; habla con la coordinación");
    }
    if (asiste && cita.getConfirmacion() == ConfirmacionCita.CONFIRMADA) {
      throw new ReglaNegocioException("Ya confirmaste tu asistencia");
    }
    cita.responderConfirmacion(asiste, ahora);
    Cita guardada = citaRepository.saveAndFlush(cita);
    eventos.publishEvent(
        EventoNotificacion.aCoordinadores(
            asiste ? TipoNotificacion.ASISTENCIA_CONFIRMADA : TipoNotificacion.CITA_DECLINADA,
            guardada.getDocente(),
            "/agenda",
            ENTIDAD_CITA,
            guardada.getIdCita(),
            "cita:" + guardada.getIdCita() + (asiste ? ":confirmada" : ":declinada")));
    return CitaResponse.desde(guardada, null);
  }

  /**
   * La coordinación registra si el docente asistió. Si asistió se le habilita su resultado; si no,
   * la cita se libera para que reserve otra.
   *
   * @throws RecursoNoEncontradoException si la cita no existe o el docente está fuera del alcance
   * @throws ReglaNegocioException si la cita no está activa, aún no es su día, ya tiene asistencia
   *     o el docente todavía no tiene las tres evaluaciones
   */
  public CitaResponse registrarAsistencia(Long idCita, boolean asistio, UsuarioSesion sesion) {
    Cita cita =
        citaRepository
            .findById(idCita)
            .orElseThrow(() -> new RecursoNoEncontradoException(CITA_NO_ENCONTRADA));
    try {
      docenteService.buscarDocenteVisible(cita.getDocente().getIdUsuario(), sesion);
    } catch (RecursoNoEncontradoException fueraDeAlcance) {
      throw new RecursoNoEncontradoException(CITA_NO_ENCONTRADA);
    }
    if (cita.getEstado() != EstadoCita.ACTIVA) {
      throw new ReglaNegocioException("La cita ya no está activa");
    }
    if (cita.getAsistencia() != null) {
      throw new ReglaNegocioException("La asistencia de esta cita ya está registrada");
    }
    LocalDateTime ahora = LocalDateTime.now(reloj);
    if (cita.getFranja().getFecha().isAfter(ahora.toLocalDate())) {
      throw new ReglaNegocioException("La asistencia se registra el día de la cita");
    }
    if (asistio
        && promedioService
                .obtenerPromedio(cita.getDocente().getIdUsuario(), cita.getPeriodo())
                .estado()
            != EstadoPromedio.COMPLETO) {
      throw new ReglaNegocioException(
          "El docente aún no tiene las tres evaluaciones; no hay resultado para socializar");
    }
    cita.registrarAsistencia(asistio, sesion.idUsuario(), ahora);
    Cita guardada = citaRepository.saveAndFlush(cita);
    eventos.publishEvent(
        EventoNotificacion.aUsuario(
            asistio ? TipoNotificacion.RESULTADO_HABILITADO : TipoNotificacion.INASISTENCIA_CITA,
            guardada.getDocente().getIdUsuario(),
            asistio ? "/mi-evaluacion" : "/mi-cita",
            ENTIDAD_CITA,
            guardada.getIdCita(),
            "cita:" + guardada.getIdCita() + ":asistencia"));
    return CitaResponse.desde(guardada, null);
  }

  private LocalDateTime inicio(Cita cita) {
    return cita.getFranja().getFecha().atTime(cita.getFranja().getHoraInicio());
  }
}
