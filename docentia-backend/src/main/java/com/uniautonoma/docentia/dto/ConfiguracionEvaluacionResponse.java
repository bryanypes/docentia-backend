package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Rol;
import java.math.BigDecimal;

/**
 * Valores que la aplicación web muestra tal como están configurados en el servidor.
 *
 * @param umbralRevision nota final por debajo de la cual el docente queda «Para revisar»
 */
public record ConfiguracionEvaluacionResponse(
    BigDecimal umbralCompromisos,
    BigDecimal umbralRevision,
    Rol rolCorrectorActas,
    BigDecimal pesoEstudiantes,
    BigDecimal pesoCoordinacion,
    BigDecimal pesoAutoevaluacion) {}
