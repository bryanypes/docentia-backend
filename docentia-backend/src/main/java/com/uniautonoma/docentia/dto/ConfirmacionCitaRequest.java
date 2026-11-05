package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.NotNull;

/** Respuesta del docente a su cita: verdadero confirma, falso la declina y libera la franja. */
public record ConfirmacionCitaRequest(@NotNull(message = "Indica si asistirás") Boolean asiste) {}
