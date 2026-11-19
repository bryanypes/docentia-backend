package com.uniautonoma.docentia.dto;

import java.math.BigDecimal;

/**
 * Acción sugerida para un docente. Es informativa: no notifica, no bloquea y no cambia nada.
 *
 * @param estadoPromedio PARCIAL si faltan fuentes y la categoría puede cambiar
 */
public record AccionSugeridaResponse(
    Long idDocente,
    String nombreDocente,
    String programa,
    EstadoPromedio estadoPromedio,
    BigDecimal promedio,
    CategoriaResponse categoria,
    String accion) {}
