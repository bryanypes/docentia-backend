package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.PromedioPonderadoResponse;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PesosFuentes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Combina las notas de las tres fuentes según el peso de cada una. */
@Component
public class CalculadoraPromedioPonderado {

  private static final int DECIMALES_PROMEDIO = 2;

  /**
   * Calcula el promedio ponderado de un docente.
   *
   * @param notasPorFuente nota de cada fuente que ya calificó; las fuentes ausentes se consideran
   *     pendientes
   * @param pesos porcentaje de cada fuente, que debe sumar 100
   * @return el promedio redondeado a dos decimales, o un resultado incompleto con las fuentes que
   *     faltan si no están las tres
   */
  public PromedioPonderadoResponse calcularPromedio(
      Map<FuenteEvaluacion, BigDecimal> notasPorFuente, PesosFuentes pesos) {
    List<FuenteEvaluacion> fuentesPendientes =
        Arrays.stream(FuenteEvaluacion.values())
            .filter(fuente -> !notasPorFuente.containsKey(fuente))
            .toList();
    if (!fuentesPendientes.isEmpty()) {
      return PromedioPonderadoResponse.incompleto(fuentesPendientes);
    }

    BigDecimal sumaPonderada = BigDecimal.ZERO;
    for (FuenteEvaluacion fuente : FuenteEvaluacion.values()) {
      sumaPonderada = sumaPonderada.add(notasPorFuente.get(fuente).multiply(pesos.pesoDe(fuente)));
    }
    return PromedioPonderadoResponse.completo(
        sumaPonderada.divide(
            PesosFuentes.TOTAL_PORCENTAJE, DECIMALES_PROMEDIO, RoundingMode.HALF_UP));
  }
}
