package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Por qué la administración reabre un formulario sin que nadie lo haya pedido; queda auditado. */
public record ReaperturaDirectaRequest(
    @NotBlank(message = "Escribe el motivo de la reapertura")
        @Size(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres")
        String motivo) {}
