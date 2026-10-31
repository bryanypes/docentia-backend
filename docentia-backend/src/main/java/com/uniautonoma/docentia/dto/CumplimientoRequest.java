package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Observación con la que se marca cumplido un compromiso. */
public record CumplimientoRequest(
    @NotBlank(message = "La observación es obligatoria")
        @Size(max = 500, message = "La observación no puede superar los 500 caracteres")
        String observacion) {}
