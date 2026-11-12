package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.NotNull;

/** Asistencia que registra la coordinación: verdadero habilita el resultado al docente. */
public record AsistenciaCitaRequest(
    @NotNull(message = "Indica si el docente asistió") Boolean asistio) {}
