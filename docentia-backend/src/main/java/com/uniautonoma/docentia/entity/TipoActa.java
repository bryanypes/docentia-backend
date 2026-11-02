package com.uniautonoma.docentia.entity;

/**
 * Las dos actas de la reunión. Con una nota menor al umbral el acta siempre es de compromiso; por
 * encima, la coordinación elige.
 */
public enum TipoActa {
  /** Plan de mejoramiento: exige al menos un compromiso con plazo. */
  COMPROMISO,
  /** Constancia de que el docente recibió la retroalimentación. */
  RETROALIMENTACION
}
