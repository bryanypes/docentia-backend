package com.uniautonoma.docentia.dto;

import java.util.List;
import java.util.Map;

/**
 * Agregado de los análisis vigentes de un periodo, por programa o por el alcance de quien consulta.
 * Cada número es la suma de los análisis individuales; cada tema dice a qué docentes se refiere
 * (solo del alcance de quien consulta), nunca los comentarios. Con menos docentes que el mínimo
 * configurado no se muestra nada (disponible falso, motivo GRUPO_PEQUENO).
 *
 * @param docentesConAnalisis docentes del alcance con un análisis vigente en el periodo
 * @param nComentarios suma de comentarios útiles analizados
 * @param nExcluidos suma de excluidos por motivo
 * @param temas suma de frecuencias por criterio, descriptor y polaridad, con cuántos docentes lo
 *     tienen
 */
public record ResumenComentariosResponse(
    boolean disponible,
    String motivo,
    Long periodoId,
    AnalisisComentariosResponse.Referencia programa,
    int docentesConAnalisis,
    int minimoDocentes,
    int nComentarios,
    Map<String, Integer> nExcluidos,
    Sentimiento sentimientoGlobal,
    List<TemaAgregado> temas,
    List<CriterioAgregado> criterios,
    Map<String, Integer> advertencias) {

  public record Sentimiento(int positivos, int negativos, int neutros, int mixtos) {}

  /**
   * @param porcentaje frecuencia sobre nComentarios, de 0 a 100
   * @param docentes docentes en cuyo análisis aparece el tema
   * @param listaDocentes quiénes son, de los que más lo mencionan a los que menos
   */
  public record TemaAgregado(
      String criterio,
      String descriptor,
      String nombre,
      String polaridad,
      int frecuencia,
      double porcentaje,
      int docentes,
      List<DocenteTema> listaDocentes) {}

  /**
   * @param menciones comentarios de sus estudiantes que tocan el tema
   */
  public record DocenteTema(Long idDocente, String nombre, int menciones) {}

  public record CriterioAgregado(
      String codigo, String nombre, int positivos, int negativos, int neutros) {}
}
