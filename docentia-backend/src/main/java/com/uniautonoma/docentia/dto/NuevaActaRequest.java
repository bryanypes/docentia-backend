package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.NotNull;

/** Docente para el que se abre el acta del periodo activo. */
public record NuevaActaRequest(@NotNull(message = "El docente es obligatorio") Long idDocente) {}
