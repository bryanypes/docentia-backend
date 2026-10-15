package com.uniautonoma.docentia.dto;

import java.math.BigDecimal;

/**
 * Un indicador con su comparación contra el periodo anterior.
 *
 * @param valor valor en el periodo consultado; nulo solo si no se puede calcular (un promedio sin
 *     docentes evaluados)
 * @param valorAnterior valor en el periodo anterior; nulo si no hay periodo anterior o no tiene
 *     datos en el alcance
 * @param variacion valor menos valorAnterior; nula si falta alguno de los dos
 */
public record IndicadorResponse(BigDecimal valor, BigDecimal valorAnterior, BigDecimal variacion) {

  public static IndicadorResponse comparar(BigDecimal valor, BigDecimal valorAnterior) {
    BigDecimal variacion =
        valor == null || valorAnterior == null ? null : valor.subtract(valorAnterior);
    return new IndicadorResponse(valor, valorAnterior, variacion);
  }
}
