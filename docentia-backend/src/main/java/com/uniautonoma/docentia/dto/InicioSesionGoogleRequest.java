package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.NotBlank;

public record InicioSesionGoogleRequest(
    @NotBlank(message = "El token de Google es obligatorio") String idToken) {}
