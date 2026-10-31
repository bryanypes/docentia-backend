package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Rol;
import java.math.BigDecimal;

/** Valores que la aplicación web muestra tal como están configurados en el servidor. */
public record ConfiguracionEvaluacionResponse(
    BigDecimal umbralCompromisos,
    Rol rolCorrectorActas,
    BigDecimal pesoEstudiantes,
    BigDecimal pesoCoordinacion,
    BigDecimal pesoAutoevaluacion) {}
