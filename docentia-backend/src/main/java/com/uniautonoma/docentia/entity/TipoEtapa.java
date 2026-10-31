package com.uniautonoma.docentia.entity;

/** Las cuatro etapas del proceso de evaluación de un periodo, en el orden en que ocurren. */
public enum TipoEtapa {
  /** Calificaciones de coordinación y vicerrectoría, y carga de la evaluación de estudiantes. */
  RECOLECCION_CALIFICACIONES("Recolección de calificaciones"),
  REVISION_HALLAZGOS("Revisión de hallazgos"),
  AGENDA_CITAS("Agenda de citas"),
  FIRMA_ACTAS("Firma de actas");

  private final String nombre;

  TipoEtapa(String nombre) {
    this.nombre = nombre;
  }

  public String getNombre() {
    return nombre;
  }
}
