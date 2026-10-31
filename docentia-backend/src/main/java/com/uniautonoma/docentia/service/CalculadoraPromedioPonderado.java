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
   * Calcula el promedio ponderado de un docente: estudiantes 50 %, coordinación 30 % y
   * autoevaluación 20 %, salvo que la vicerrectoría configure otros pesos.
   *
   * @param notasPorFuente nota de cada fuente que ya calificó
   * @param pesos porcentaje de cada fuente, que debe sumar 100
   * @return el promedio redondeado a dos decimales; si faltan fuentes los pesos se renormalizan
   *     entre las disponibles y queda PARCIAL; sin ninguna fuente, SIN_NOTA
   */
  public PromedioPonderadoResponse calcularPromedio(
      Map<FuenteEvaluacion, BigDecimal> notasPorFuente, PesosFuentes pesos) {
    List<FuenteEvaluacion> pendientes =
        Arrays.stream(FuenteEvaluacion.values())
            .filter(fuente -> !notasPorFuente.containsKey(fuente))
            .toList();
    BigDecimal sumaPonderada = BigDecimal.ZERO;
    BigDecimal sumaPesos = BigDecimal.ZERO;
    for (Map.Entry<FuenteEvaluacion, BigDecimal> nota : notasPorFuente.entrySet()) {
      BigDecimal peso = pesos.pesoDe(nota.getKey());
      sumaPonderada = sumaPonderada.add(nota.getValue().multiply(peso));
      sumaPesos = sumaPesos.add(peso);
    }
    if (sumaPesos.signum() == 0) {
      return PromedioPonderadoResponse.sinNota();
    }
    BigDecimal valor = sumaPonderada.divide(sumaPesos, DECIMALES_PROMEDIO, RoundingMode.HALF_UP);
    return pendientes.isEmpty()
        ? PromedioPonderadoResponse.completo(valor)
        : PromedioPonderadoResponse.parcial(valor, pendientes);
  }
}
