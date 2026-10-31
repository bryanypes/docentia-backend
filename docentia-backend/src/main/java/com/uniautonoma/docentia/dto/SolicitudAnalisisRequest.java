package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.NotNull;

public record SolicitudAnalisisRequest(
    @NotNull(message = "El docente es obligatorio") Long docenteId,
    @NotNull(message = "El periodo es obligatorio") Long periodoId) {}
