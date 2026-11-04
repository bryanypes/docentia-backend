package com.uniautonoma.docentia.service.analisis.local;

import java.util.List;
import java.util.Map;

/**
 * Salida estructurada del análisis, común a los dos modelos. Se guarda como JSON junto a la
 * solicitud.
 *
 * @param tipoModelo REGLAS o LLM
 * @param criterios agregado por criterio de la Res. 074
 */
public record ResultadoAnalisis(
    String tipoModelo,
    String versionModelo,
    String versionLexicoOPrompt,
    int nComentarios,
    Map<String, Integer> nExcluidos,
    Sentimiento sentimientoGlobal,
    List<Tema> temas,
    Aspectos aspectos,
    List<Tendencia> tendencias,
    List<Advertencia> advertencias,
    List<Criterio> criterios,
    Map<String, Object> parametros) {

  public record Sentimiento(int positivos, int negativos, int neutros, int mixtos, double indice) {}

  /**
   * Un tema: criterio, descriptor y polaridad.
   *
   * @param frecuencia comentarios útiles que lo mencionan
   * @param porcentaje sobre los comentarios útiles
   * @param citas fragmentos que existen textualmente en la entrada anonimizada
   */
  public record Tema(
      String criterio,
      String descriptor,
      String nombre,
      String polaridad,
      int frecuencia,
      double porcentaje,
      double intensidad,
      double relevancia,
      boolean recurrente,
      List<Cita> citas) {}

  public record Cita(Long comentarioId, String fragmento) {}

  public record Aspectos(List<Tema> fortalezas, List<Tema> oportunidades) {}

  /**
   * @param cambio NUEVO, SE_MANTIENE, EN_AUMENTO, EN_DISMINUCION o SIN_REFERENCIA
   */
  public record Tendencia(
      String criterio,
      String descriptor,
      String polaridad,
      Double porcentajeAnterior,
      double porcentajeActual,
      String cambio) {}

  public record Advertencia(String tipo, int cantidad, String mensaje) {}

  public record Criterio(
      String codigo, String nombre, int positivos, int negativos, int neutros, double balance) {}
}
