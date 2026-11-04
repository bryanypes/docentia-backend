package com.uniautonoma.docentia.dto;

/**
 * Cuántos comentarios de estudiantes de un docente contienen el término buscado. Nunca lleva el
 * texto de los comentarios.
 */
public record CoincidenciaComentariosResponse(Long idDocente, long coincidencias) {}
