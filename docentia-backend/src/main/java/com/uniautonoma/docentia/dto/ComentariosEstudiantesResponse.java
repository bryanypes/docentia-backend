package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.FuenteComentarios;
import java.util.List;

/**
 * Comentarios anonimizados que entraron al análisis vigente. No incluye los excluidos (ofensivos,
 * sobre la persona, situaciones graves, instrucciones al modelo, duplicados ni muy cortos).
 *
 * @param disponible falso si todavía no hay un análisis completado
 * @param motivo por qué no está disponible: el mismo estado del análisis (EN_COLA, INSUFICIENTE,
 *     NO_DISPONIBLE_AUN...)
 */
public record ComentariosEstudiantesResponse(
    boolean disponible,
    String motivo,
    Long docenteId,
    Long periodoId,
    FuenteComentarios fuente,
    Integer version,
    int pagina,
    int tamano,
    long total,
    List<Comentario> comentarios) {

  /**
   * @param indice posición en el orden de la huella, estable dentro de la versión
   * @param sentimiento POSITIVO, NEGATIVO, NEUTRO o MIXTO
   */
  public record Comentario(
      int indice, String texto, String sentimiento, List<EtiquetaComentario> etiquetas) {}

  /**
   * @param polaridad POSITIVO, NEGATIVO o NEUTRO
   */
  public record EtiquetaComentario(
      String criterio, String descriptor, String nombre, String polaridad) {}
}
