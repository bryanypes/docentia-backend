package com.uniautonoma.docentia.entity;

/**
 * Estado general del proceso de un docente en el periodo, en orden. No se guarda: se deduce del
 * promedio, la cita y el acta, así que no puede quedar desfasado.
 */
public enum EstadoProcesoEvaluacion {
  EVALUACIONES_EN_CURSO("Evaluaciones en curso", 1),
  PENDIENTE_AUTOEVALUACION("Falta la autoevaluación", 2),
  EVALUACION_COMPLETA("Evaluación completa · sin cita", 3),
  NO_ASISTIO("No asistió · reprogramar", 3),
  CITA_AGENDADA("Cita agendada · sin confirmar", 4),
  ASISTENCIA_CONFIRMADA("Asistencia confirmada", 5),
  EN_REUNION("En reunión · resultado habilitado", 6),
  ACTA_EN_FIRMA("Acta en firma", 7),
  FINALIZADO("Proceso finalizado", 8);

  private final String nombre;
  private final int paso;

  EstadoProcesoEvaluacion(String nombre, int paso) {
    this.nombre = nombre;
    this.paso = paso;
  }

  public String getNombre() {
    return nombre;
  }

  /** Paso de la línea de tiempo, de 1 a 8. NO_ASISTIO vuelve al paso de agendar. */
  public int getPaso() {
    return paso;
  }
}
