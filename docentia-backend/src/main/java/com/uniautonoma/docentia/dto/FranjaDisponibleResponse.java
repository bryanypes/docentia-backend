package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.FranjaHoraria;
import java.time.LocalDate;
import java.time.LocalTime;

/** Franja que el docente puede reservar. */
public record FranjaDisponibleResponse(
    Long idFranja,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin,
    String nombreCoordinador) {

  public static FranjaDisponibleResponse desde(FranjaHoraria franja) {
    return new FranjaDisponibleResponse(
        franja.getIdFranja(),
        franja.getFecha(),
        franja.getHoraInicio(),
        franja.getHoraFin(),
        franja.getCoordinador().getNombreCompleto());
  }
}
