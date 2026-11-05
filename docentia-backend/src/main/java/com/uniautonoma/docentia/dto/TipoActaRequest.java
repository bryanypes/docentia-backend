package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.TipoActa;
import jakarta.validation.constraints.NotNull;

/** Tipo del acta: de compromiso o de retroalimentación. */
public record TipoActaRequest(@NotNull(message = "El tipo de acta es obligatorio") TipoActa tipo) {}
