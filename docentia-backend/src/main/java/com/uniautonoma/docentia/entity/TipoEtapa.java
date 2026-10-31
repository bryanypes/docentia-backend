package com.uniautonoma.docentia.entity;

/** Las cuatro etapas del proceso de evaluación de un periodo, en el orden en que ocurren. */
public enum TipoEtapa {
  /** Calificaciones de coordinación y vicerrectoría, y carga de la evaluación de estudiantes. */
  RECOLECCION_CALIFICACIONES("Recolección de calificaciones", "Semanas 11 a 14"),
  REVISION_HALLAZGOS("Revisión de hallazgos", "Semana 15 (consolidación)"),
  AGENDA_CITAS("Agenda de citas", "Semana 16 y posteriores"),
  FIRMA_ACTAS("Firma de actas", "Semana 16 y posteriores");

  private final String nombre;
  private final String semanasResolucion;

  TipoEtapa(String nombre, String semanasResolucion) {
    this.nombre = nombre;
    this.semanasResolucion = semanasResolucion;
  }

  /** Semanas académicas de la Resolución 074; informativo, las ventanas las dan las fechas. */
  public String getSemanasResolucion() {
    return semanasResolucion;
  }

  public String getNombre() {
    return nombre;
  }
}
