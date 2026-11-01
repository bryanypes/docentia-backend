package com.uniautonoma.docentia.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Borrador de un formulario.
 *
 * @param respuestas valor por código de descriptor; puede estar incompleto
 * @param comentarios comentario opcional por código de criterio
 */
public record FormularioRequest(
    Map<String, BigDecimal> respuestas, Map<String, String> comentarios) {}
