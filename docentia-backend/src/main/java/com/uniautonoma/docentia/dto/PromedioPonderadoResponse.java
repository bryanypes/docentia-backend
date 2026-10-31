package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado del promedio ponderado. Cuando falta alguna fuente el valor es nulo: un promedio
 * parcial se confundiría con uno definitivo.
 */
public record PromedioPonderadoResponse(
    EstadoPromedio estado, BigDecimal valor, List<FuenteEvaluacion> fuentesPendientes) {

  public static PromedioPonderadoResponse completo(BigDecimal valor) {
    return new PromedioPonderadoResponse(EstadoPromedio.COMPLETO, valor, List.of());
  }

  public static PromedioPonderadoResponse incompleto(List<FuenteEvaluacion> fuentesPendientes) {
    return new PromedioPonderadoResponse(EstadoPromedio.INCOMPLETO, null, fuentesPendientes);
  }
}
