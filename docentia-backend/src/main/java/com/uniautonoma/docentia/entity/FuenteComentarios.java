package com.uniautonoma.docentia.entity;

/** De dónde salen los comentarios que se analizan. */
public enum FuenteComentarios {
  /** Cargados con el CSV de la encuesta institucional. */
  CSV,
  /** Escritos en el formulario anónimo de heteroevaluación; se leen solo al analizar. */
  FORMULARIO
}
