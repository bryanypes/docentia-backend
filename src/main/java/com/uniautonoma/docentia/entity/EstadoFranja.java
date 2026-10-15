package com.uniautonoma.docentia.entity;

public enum EstadoFranja {
  /** Disponible para reservar (si no tiene cita activa). */
  ABIERTA,
  /** El coordinador la cerró; no se puede reservar. */
  CERRADA
}
