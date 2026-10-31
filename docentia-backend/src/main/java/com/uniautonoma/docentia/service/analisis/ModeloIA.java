package com.uniautonoma.docentia.service.analisis;

import java.util.List;

/**
 * Modelo de lenguaje que propone hallazgos a partir de comentarios ya filtrados y anonimizados. La
 * implementación se elige con docentia.ia.proveedor.
 */
public interface ModeloIA {

  /**
   * Pide el análisis.
   *
   * @return el texto que devolvió el modelo (se espera JSON) y el modelo que lo generó
   * @throws ModeloIAException si el proveedor no responde, responde con error o se agota el tiempo
   */
  RespuestaModelo analizar(PeticionAnalisis peticion);

  /** Comentario listo para enviar: sin datos identificables. */
  record ComentarioAnonimo(Long id, String texto) {}

  /**
   * Lo que se envía al modelo.
   *
   * @param instruccion texto de instrucción versionado
   * @param comentarios comentarios anonimizados con su identificador para citarlos
   */
  record PeticionAnalisis(String instruccion, List<ComentarioAnonimo> comentarios) {}

  record RespuestaModelo(String contenido, String modelo) {}

  /** El proveedor falló; el mensaje es técnico y solo va al registro, no al usuario. */
  class ModeloIAException extends RuntimeException {

    public ModeloIAException(String mensaje, Throwable causa) {
      super(mensaje, causa);
    }
  }
}
