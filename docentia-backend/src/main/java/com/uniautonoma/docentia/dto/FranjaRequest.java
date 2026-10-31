package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Franja a abrir. La duración es fija: horaFin debe ser horaInicio más 30 minutos.
 *
 * @param horaInicio en formato "HH:mm"
 */
public record FranjaRequest(
    @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
    @NotNull(message = "La hora de inicio es obligatoria") LocalTime horaInicio,
    @NotNull(message = "La hora de fin es obligatoria") LocalTime horaFin) {}
