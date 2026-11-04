package com.uniautonoma.docentia.service.analisis.local;

import java.util.LinkedHashMap;
import java.util.Map;

/** Los 11 descriptores que responde el estudiante (Anexo 1), con su criterio y un nombre corto. */
public final class CatalogoDescriptores {

  public record Descriptor(String codigo, String criterio, String nombre) {}

  public static final Map<String, String> CRITERIOS =
      Map.of(
          "C1", "Capacidad para planear procesos de docencia",
          "C2", "Capacidad para ejecutar procesos de docencia",
          "C3", "Capacidad para evaluar procesos de docencia",
          "C4", "Sensibilidad social",
          "C5", "Creatividad, dinamismo e innovación",
          "C6", "Capacidad para aprender, desaprender y reaprender");

  public static final Map<String, Descriptor> DESCRIPTORES = new LinkedHashMap<>();

  static {
    agregar("D02", "C2", "Socialización de resultados de aprendizaje y criterios de evaluación");
    agregar("D03", "C2", "Formación en emprendimiento y responsabilidad social o ambiental");
    agregar("D04", "C2", "Fomento de la investigación y el pensamiento crítico");
    agregar("D05", "C2", "Claridad al explicar ideas e instrucciones");
    agregar("D06", "C3", "Evaluaciones alineadas con lo enseñado");
    agregar("D07", "C3", "Retroalimentación sobre los resultados");
    agregar("D08", "C2", "Acompañamiento y seguimiento al aprendizaje");
    agregar("D09", "C4", "Trato respetuoso y cordial");
    agregar("D10", "C4", "Atención a las necesidades de los estudiantes");
    agregar("D11", "C5", "Clases creativas y dinámicas");
    agregar("D12", "C5", "Uso de metodologías, herramientas y tecnologías");
  }

  private CatalogoDescriptores() {}

  private static void agregar(String codigo, String criterio, String nombre) {
    DESCRIPTORES.put(codigo, new Descriptor(codigo, criterio, nombre));
  }
}
