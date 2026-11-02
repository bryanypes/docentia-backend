package com.uniautonoma.docentia.dto;

import java.util.List;

/** Lo que el estudiante tiene por evaluar. Nunca lleva notas. */
public record EvaluacionesEstudianteResponse(
    PeriodoResumenResponse periodo, long pendientes, List<Asignacion> evaluaciones) {

  /**
   * @param estado PENDIENTE o ENVIADA
   */
  public record Asignacion(
      Long idAsignacion,
      String codigoCurso,
      String curso,
      String grupo,
      String docente,
      String programa,
      String estado) {}
}
