package com.uniautonoma.docentia.dto;

import java.util.List;

/**
 * Clases en las que el estudiante está matriculado, con el avance de cada formulario HETERO. Sin
 * puntajes.
 */
public record HeteroevaluacionListaResponse(
    PeriodoResumenResponse periodo,
    int itemsHeteroevaluacion,
    long pendientes,
    List<ClaseDocenteAvance> clases) {

  public record ClaseDocenteAvance(
      Long idClase,
      Long idDocente,
      String nombreDocente,
      String nombreCurso,
      String grupo,
      String jornada,
      String nombrePrograma,
      String estado,
      int respondidos) {}
}
