package com.uniautonoma.docentia.service.analisis.local;

import java.util.List;

/** Analiza comentarios ya filtrados y anonimizados. */
public interface AnalizadorComentarios {

  /** Un comentario útil, con un identificador estable dentro del análisis. */
  record Comentario(Long id, String texto) {}

  /** Etiqueta de un comentario: descriptor, criterio y polaridad. */
  record Etiqueta(String criterio, String descriptor, String polaridad, double intensidad) {}

  /** Lo que se dice de cada comentario, para la vista de comentarios. */
  record Etiquetado(Long id, String sentimiento, List<Etiqueta> etiquetas) {}

  record Salida(ResultadoAnalisis resultado, List<Etiquetado> etiquetados) {}

  Salida analizar(List<Comentario> comentarios, ResultadoAnalisis anterior);
}
