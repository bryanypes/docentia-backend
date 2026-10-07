package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Dimension;
import java.math.BigDecimal;

/** Resultado de una dimensión ya ponderado entre las tres fuentes, sin el detalle de cada una. */
public record ResultadoDimensionResponse(
    Dimension dimension, String nombre, EstadoPromedio estado, BigDecimal resultado) {

  public static ResultadoDimensionResponse desde(
      Dimension dimension, PromedioPonderadoResponse resultado) {
    return new ResultadoDimensionResponse(
        dimension, dimension.getNombre(), resultado.estado(), resultado.valor());
  }
}
