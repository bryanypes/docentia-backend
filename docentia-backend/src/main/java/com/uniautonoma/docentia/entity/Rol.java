package com.uniautonoma.docentia.entity;

import java.util.Set;

public enum Rol {
  /**
   * Coordina una facultad y además es docente: recibe evaluación (estudiantes, autoevaluación y la
   * vicerrectoría en el lugar de la coordinación) y tiene su cita y su acta con la vicerrectoría.
   */
  COORDINADOR,
  VICERRECTOR,
  /** Usuarios, catálogos, ponderaciones y fechas; no ve notas, comentarios, análisis ni actas. */
  ADMINISTRADOR,
  /** Solo lectura de paneles agregados. */
  VISUALIZADOR,
  DOCENTE,
  ESTUDIANTE;

  /** Roles que reciben evaluación docente. */
  public static final Set<Rol> EVALUABLES = Set.of(DOCENTE, COORDINADOR);

  public boolean esEvaluable() {
    return EVALUABLES.contains(this);
  }

  /**
   * Quién evalúa (coevaluación, cita y acta) a una persona con este rol: al docente lo evalúa la
   * coordinación de su facultad; al coordinador, la vicerrectoría.
   */
  public Rol evaluador() {
    return switch (this) {
      case DOCENTE -> COORDINADOR;
      case COORDINADOR -> VICERRECTOR;
      default -> throw new IllegalStateException("El rol " + this + " no recibe evaluación");
    };
  }
}
