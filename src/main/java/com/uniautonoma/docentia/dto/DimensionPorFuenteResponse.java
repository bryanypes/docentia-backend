package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Dimension;
import java.math.BigDecimal;

/** Una dimensión con la nota de cada fuente. Solo la ven coordinación y vicerrectoría. */
public record DimensionPorFuenteResponse(
    Dimension dimension,
    String nombre,
    BigDecimal notaEstudiantes,
    BigDecimal notaCoordinacion,
    BigDecimal notaVicerrectoria,
    EstadoPromedio estado,
    BigDecimal resultado) {}
