package com.uniautonoma.docentia.entity;

/** Lo que la coordinación registra en la reunión. */
public enum AsistenciaCita {
  /** El docente estuvo con la coordinación: se le habilita su resultado. */
  ASISTIO,
  /** No llegó: la cita se libera y el docente debe reservar otra. */
  NO_ASISTIO
}
