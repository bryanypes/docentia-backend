package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InicioSesionGoogleRequest(
    @NotBlank(message = "El token de Google es obligatorio")
        @Size(max = 4096, message = "El token de Google es demasiado largo")
        String idToken) {}
