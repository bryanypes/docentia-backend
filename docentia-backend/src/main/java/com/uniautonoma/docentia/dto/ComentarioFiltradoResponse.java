package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.ComentarioFiltrado;
import java.time.LocalDateTime;

/** Comentario excluido del análisis, para que coordinación sepa qué no se envió y por qué. */
public record ComentarioFiltradoResponse(
    Long idComentario, String texto, String categoria, String regla, LocalDateTime fechaFiltro) {

  public static ComentarioFiltradoResponse desde(ComentarioFiltrado filtrado) {
    return new ComentarioFiltradoResponse(
        filtrado.getComentario().getIdComentario(),
        filtrado.getComentario().getTexto(),
        filtrado.getCategoria(),
        filtrado.getRegla(),
        filtrado.getFechaFiltro());
  }
}
