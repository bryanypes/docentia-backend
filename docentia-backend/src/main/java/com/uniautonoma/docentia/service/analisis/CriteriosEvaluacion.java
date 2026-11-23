package com.uniautonoma.docentia.service.analisis;

import java.util.Map;

/**
 * Los seis criterios de la evaluación docente (Anexo 1, Res. 074), como en criterios_evaluacion.
 */
public final class CriteriosEvaluacion {

  private static final Map<String, String> NOMBRES =
      Map.of(
          "C1", "Capacidad para planear procesos de docencia",
          "C2", "Capacidad para ejecutar procesos de docencia",
          "C3", "Capacidad para evaluar procesos de docencia",
          "C4", "Sensibilidad social",
          "C5", "Creatividad, dinamismo e innovación",
          "C6", "Capacidad para aprender, desaprender y reaprender");

  private CriteriosEvaluacion() {}

  public static boolean existe(String codigo) {
    return codigo != null && NOMBRES.containsKey(codigo);
  }

  /** Nombre del criterio, o nulo si el código no es de un criterio. */
  public static String nombre(String codigo) {
    return codigo == null ? null : NOMBRES.get(codigo);
  }
}
