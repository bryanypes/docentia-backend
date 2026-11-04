package com.uniautonoma.docentia.entity;

/** Respuesta del docente al aviso de su cita. Sin respuesta, el campo queda nulo. */
public enum ConfirmacionCita {
  CONFIRMADA,
  /** El docente no irá: la cita se libera y la franja queda para otro docente. */
  DECLINADA
}
