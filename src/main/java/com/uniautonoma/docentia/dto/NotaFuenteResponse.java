package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import java.math.BigDecimal;

/**
 * @param nota promedio de los cinco aspectos de la fuente, o nulo si aún no ha calificado
 * @param peso porcentaje con el que la fuente entra al promedio ponderado
 */
public record NotaFuenteResponse(FuenteEvaluacion fuente, BigDecimal nota, BigDecimal peso) {}
