package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.CitaResponse;
import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.EstadoCita;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import com.uniautonoma.docentia.entity.MotivoLiberacion;
import com.uniautonoma.docentia.entity.TipoNotificacion;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.CitaRepository;
import com.uniautonoma.docentia.repository.FranjaHorariaRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.notificacion.EventoNotificacion;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La coordinación cancela o cambia de horario una cita reservada en sus propias franjas. En los dos
 * casos el docente recibe el aviso y, si cambia de horario, vuelve a confirmar su asistencia.
 */
@Service
@Transactional
public class GestionCitaService {

  private static final String CITA_NO_ENCONTRADA = "Cita no encontrada";
  private static final String ENTIDAD_CITA = "CITA";

  private final CitaRepository citaRepository;
  private final FranjaHorariaRepository franjaRepository;
  private final ApplicationEventPublisher eventos;
  private final Clock reloj;

  public GestionCitaService(
      CitaRepository citaRepository,
      FranjaHorariaRepository franjaRepository,
      ApplicationEventPublisher eventos,
      Clock reloj) {
    this.citaRepository = citaRepository;
    this.franjaRepository = franjaRepository;
    this.eventos = eventos;
    this.reloj = reloj;
  }

  /**
   * Cancela la cita: la franja queda libre y el docente debe reservar otra.
   *
   * @throws RecursoNoEncontradoException si la cita no existe o la franja es de otro coordinador
   * @throws ReglaNegocioException si la cita ya no está activa o ya tiene asistencia
   */
  public CitaResponse cancelar(Long idCita, UsuarioSesion sesion) {
    Cita cita = buscarGestionable(idCita, sesion);
    cita.liberar(MotivoLiberacion.CANCELADA_POR_COORDINACION);
    Cita guardada = citaRepository.saveAndFlush(cita);
    avisar(TipoNotificacion.CITA_CANCELADA, guardada, ":cancelada");
    return CitaResponse.desde(guardada, null);
  }

  /**
   * Mueve la cita a otra franja propia, abierta, futura y libre. La cita anterior queda liberada en
   * el historial y la nueva empieza sin confirmación.
   *
   * @throws RecursoNoEncontradoException si la cita o la franja no existen o son de otro
   *     coordinador
   * @throws ReglaNegocioException si la cita no se puede mover o la franja no está disponible
   */
  public CitaResponse reprogramar(Long idCita, Long idFranja, UsuarioSesion sesion) {
    Cita cita = buscarGestionable(idCita, sesion);
    FranjaHoraria franja =
        franjaRepository
            .findByIdFranjaAndCoordinadorIdUsuario(idFranja, sesion.idUsuario())
            .orElseThrow(() -> new RecursoNoEncontradoException("Franja no encontrada"));
    if (franja.getIdFranja().equals(cita.getFranja().getIdFranja())) {
      throw new ReglaNegocioException("La cita ya está en esa franja");
    }
    if (!franja.getPeriodo().getIdPeriodo().equals(cita.getPeriodo().getIdPeriodo())) {
      throw new ReglaNegocioException("La franja es de otro periodo");
    }
    if (!franja.estaAbierta()) {
      throw new ReglaNegocioException("La franja está cerrada");
    }
    if (!franja.getFecha().atTime(franja.getHoraInicio()).isAfter(LocalDateTime.now(reloj))) {
      throw new ReglaNegocioException("La franja ya pasó");
    }
    if (citaRepository.existsByFranjaIdFranjaAndEstado(idFranja, EstadoCita.ACTIVA)) {
      throw new ReglaNegocioException("La franja ya fue reservada por otro docente");
    }

    cita.liberar(MotivoLiberacion.REPROGRAMADA_POR_COORDINACION);
    citaRepository.saveAndFlush(cita);
    try {
      Cita nueva = citaRepository.saveAndFlush(new Cita(franja, cita.getDocente()));
      avisar(TipoNotificacion.CITA_CAMBIADA, nueva, ":reprogramada");
      return CitaResponse.desde(nueva, cita.getIdCita());
    } catch (DataIntegrityViolationException choque) {
      throw new ReglaNegocioException(
          "No se pudo cambiar el horario porque la franja cambió al mismo tiempo; vuelve a"
              + " intentarlo");
    }
  }

  private Cita buscarGestionable(Long idCita, UsuarioSesion sesion) {
    Cita cita =
        citaRepository
            .findById(idCita)
            .filter(c -> c.getFranja().getCoordinador().getIdUsuario().equals(sesion.idUsuario()))
            .orElseThrow(() -> new RecursoNoEncontradoException(CITA_NO_ENCONTRADA));
    if (cita.getEstado() != EstadoCita.ACTIVA) {
      throw new ReglaNegocioException("La cita ya no está activa");
    }
    if (cita.getAsistencia() != null) {
      throw new ReglaNegocioException("La reunión ya ocurrió; la asistencia está registrada");
    }
    return cita;
  }

  private void avisar(TipoNotificacion tipo, Cita cita, String sufijo) {
    eventos.publishEvent(
        EventoNotificacion.aUsuario(
            tipo,
            cita.getDocente().getIdUsuario(),
            "/mi-cita",
            ENTIDAD_CITA,
            cita.getIdCita(),
            "cita:" + cita.getIdCita() + sufijo));
  }
}
