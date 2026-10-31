package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Texto corregido de un hallazgo. */
public record EdicionHallazgoRequest(
    @NotBlank(message = "El texto es obligatorio")
        @Size(max = 1000, message = "El texto no puede superar los 1000 caracteres")
        String texto) {}
