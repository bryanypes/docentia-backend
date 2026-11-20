package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.Size;

/** Lo que se habló en la reunión de retroalimentación; vacío para quitarlo. */
public record ObservacionesActaRequest(
    @Size(max = 2000, message = "Las observaciones admiten hasta 2000 caracteres") String texto) {}
