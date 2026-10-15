package com.uniautonoma.docentia.entity;

public enum EstadoSolicitudAnalisis {
  /** Registrada, esperando al procesador. */
  EN_COLA,
  /** El procesador la tomó y está generando el borrador. */
  EN_PROCESO,
  /** Hay borrador. */
  COMPLETADO,
  /** No se pudo generar el borrador; el mensaje dice qué hacer. */
  ERROR,
  /** El docente no tiene suficientes encuestas cerradas; no se genera borrador. */
  INSUFICIENTE;

  public boolean estaEnCurso() {
    return this == EN_COLA || this == EN_PROCESO;
  }
}
