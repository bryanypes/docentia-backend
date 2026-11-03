package com.uniautonoma.docentia.entity;

public enum MotivoLiberacion {
  /** El docente reservó otra franja en el mismo periodo. */
  REEMPLAZADA,
  /** El docente la liberó a propósito (F3, todavía sin endpoint). */
  LIBERADA_POR_DOCENTE,
  /** El docente declinó la cita al confirmar su asistencia. */
  DECLINADA_POR_DOCENTE,
  /** La coordinación registró que el docente no asistió. */
  INASISTENCIA,
  /** La coordinación canceló la cita y la franja quedó libre. */
  CANCELADA_POR_COORDINACION,
  /** La coordinación movió la cita a otra franja; la nueva es otra cita. */
  REPROGRAMADA_POR_COORDINACION
}
