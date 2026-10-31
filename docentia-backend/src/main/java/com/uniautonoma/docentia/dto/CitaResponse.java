package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.EstadoCita;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Cita del docente.
 *
 * @param idCitaLiberada al reservar: la cita anterior del docente en el periodo que quedó liberada,
 *     o nula si no tenía
 */
public record CitaResponse(
    Long idCita,
    EstadoCita estado,
    LocalDateTime fechaReserva,
    Long idFranja,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin,
    String nombreCoordinador,
    Long idCitaLiberada) {

  public static CitaResponse desde(Cita cita, Long idCitaLiberada) {
    FranjaHoraria franja = cita.getFranja();
    return new CitaResponse(
        cita.getIdCita(),
        cita.getEstado(),
        cita.getFechaReserva(),
        franja.getIdFranja(),
        franja.getFecha(),
        franja.getHoraInicio(),
        franja.getHoraFin(),
        franja.getCoordinador().getNombreCompleto(),
        idCitaLiberada);
  }
}
