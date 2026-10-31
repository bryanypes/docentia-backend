package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.Size;

/** Motivo opcional para descartar un hallazgo. */
public record DescarteHallazgoRequest(
    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres") String motivo) {}
