package com.uniautonoma.docentia.entity;

public enum Rol {
  COORDINADOR,
  VICERRECTOR,
  /** Usuarios, catálogos, ponderaciones y fechas; no ve notas, comentarios, análisis ni actas. */
  ADMINISTRADOR,
  /** Solo lectura de paneles agregados. */
  VISUALIZADOR,
  DOCENTE,
  ESTUDIANTE
}
