package com.uniautonoma.docentia.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Envío de la heteroevaluación: los 11 ítems y un comentario obligatorio (mínimo 10 caracteres).
 */
public record EnvioEstudianteRequest(Map<String, BigDecimal> respuestas, String comentario) {}
