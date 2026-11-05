package com.uniautonoma.docentia.service.analisis.local;

import com.fasterxml.jackson.annotation.JsonCreator;
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
    Map<String, Object> parametros,
    LimitesAplicados limitesAplicados,
    Boolean desdeCache,
    String generadoEn,
    String resumenNarrativo) {

  @JsonCreator
  public ResultadoAnalisis {}

  public ResultadoAnalisis(
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
    this(
        tipoModelo,
        versionModelo,
        versionLexicoOPrompt,
        nComentarios,
        nExcluidos,
        sentimientoGlobal,
        temas,
        aspectos,
        tendencias,
        advertencias,
        criterios,
        parametros,
        null,
        null,
        null,
        null);
  }

  /** Copia con los datos de la corrida: topes aplicados, caché, fecha y resumen del modelo. */
  public ResultadoAnalisis conCorrida(
      LimitesAplicados limites, boolean cache, String fecha, String resumen) {
    return new ResultadoAnalisis(
        tipoModelo,
        versionModelo,
        versionLexicoOPrompt,
        nComentarios,
        nExcluidos,
        sentimientoGlobal,
        temas,
        aspectos,
        tendencias,
        advertencias,
        criterios,
        parametros,
        limites,
        cache,
        fecha,
        resumen);
  }

  /**
   * Topes que se aplicaron, para que la interfaz no afirme que el modelo de lenguaje leyó todo.
   *
   * @param comentariosAnalizados útiles que leyó y contó el modelo local (todos)
   * @param comentariosMuestreados los que vio el modelo de lenguaje; igual a los analizados si no
   *     hubo modelo de lenguaje
   * @param muestreado si el modelo de lenguaje vio solo una muestra
   * @param recortados comentarios enviados recortados por largo
   * @param descartadosRelleno quitados antes de enviar por quedar sin contenido o casi duplicados
   * @param periodosReferencia ids de los periodos anteriores usados para las tendencias
   */
  public record LimitesAplicados(
      int comentariosAnalizados,
      int comentariosMuestreados,
      boolean muestreado,
      int recortados,
      int descartadosRelleno,
      int maxComentariosModelo,
      int maxCaracteresComentario,
      List<Long> periodosReferencia) {}

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
