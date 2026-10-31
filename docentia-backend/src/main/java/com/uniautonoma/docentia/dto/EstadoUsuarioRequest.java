package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.NotNull;

/** Activa o desactiva una cuenta. */
public record EstadoUsuarioRequest(
    @NotNull(message = "El estado es obligatorio") Boolean esActivo) {}
