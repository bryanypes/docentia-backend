package com.uniautonoma.docentia.entity;

public enum EstadoHallazgo {
  /** Tiene al menos una cita verificada; falta que el coordinador lo revise. */
  PENDIENTE_VALIDACION,
  /** Ninguna de sus citas aparece en los comentarios: no se puede validar. */
  SIN_CITA_VERIFICADA,
  /** El coordinador confirmó que es válido. */
  VALIDADO
}
