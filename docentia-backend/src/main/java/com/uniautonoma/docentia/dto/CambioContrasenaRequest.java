package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambio de la contraseña propia: la actual para confirmar identidad y la nueva. */
public record CambioContrasenaRequest(
    @NotBlank(message = "Escribe tu contraseña actual") String contrasenaActual,
    @NotBlank(message = "Escribe la contraseña nueva")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String contrasenaNueva) {}
