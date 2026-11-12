package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.NotNull;

/** Franja propia y libre a la que la coordinación mueve una cita. */
public record ReprogramacionCitaRequest(
    @NotNull(message = "Elige la nueva franja") Long idFranja) {}
