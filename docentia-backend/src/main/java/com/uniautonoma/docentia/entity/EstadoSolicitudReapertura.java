package com.uniautonoma.docentia.entity;

/** Recorrido de una solicitud para corregir un formulario ya enviado. */
public enum EstadoSolicitudReapertura {
  /** Espera la verificación del superior (coordinación o vicerrectoría). */
  PENDIENTE_APROBACION,
  /** El superior la aprobó: espera que la administración reabra el formulario. */
  APROBADA,
  /** El superior o la administración la rechazaron. */
  RECHAZADA,
  /** La administración reabrió el formulario: el solicitante ya puede corregirlo. */
  ATENDIDA
}
