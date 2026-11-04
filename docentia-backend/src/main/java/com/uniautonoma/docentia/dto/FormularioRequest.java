package com.uniautonoma.docentia.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Borrador de un formulario.
 *
 * @param respuestas valor por código de descriptor; puede estar incompleto
 * @param comentarios comentario opcional por código de criterio (formato anterior; la pantalla ya no
 *     lo envía)
 * @param comentario comentario general opcional, al final del formulario; entra al análisis
 */
public record FormularioRequest(
    Map<String, BigDecimal> respuestas, Map<String, String> comentarios, String comentario) {

  public FormularioRequest(Map<String, BigDecimal> respuestas, Map<String, String> comentarios) {
    this(respuestas, comentarios, null);
  }
}
