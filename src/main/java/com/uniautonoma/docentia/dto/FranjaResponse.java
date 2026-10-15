package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.EstadoFranja;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Franja con su reserva, si la tiene.
 *
 * @param reserva cita activa en la franja, o nula si está libre
 */
public record FranjaResponse(
    Long idFranja,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin,
    EstadoFranja estado,
    boolean reservada,
    Reserva reserva) {

  public static FranjaResponse desde(FranjaHoraria franja, Cita citaActiva) {
    return new FranjaResponse(
        franja.getIdFranja(),
        franja.getFecha(),
        franja.getHoraInicio(),
        franja.getHoraFin(),
        franja.getEstado(),
        citaActiva != null,
        citaActiva == null
            ? null
            : new Reserva(
                citaActiva.getIdCita(),
                citaActiva.getDocente().getIdUsuario(),
                citaActiva.getDocente().getNombreCompleto()));
  }

  public record Reserva(Long idCita, Long idDocente, String nombreDocente) {}
}
