package com.uniautonoma.docentia.dto;

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
    CategoriaResponse categoria,
    String accion) {}
