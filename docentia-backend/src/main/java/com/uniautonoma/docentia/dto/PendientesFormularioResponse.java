package com.uniautonoma.docentia.dto;

import java.util.List;

/**
 * Avance de la coevaluación, sin puntajes.
 *
 * @param docentes detalle para el coordinador; vacío para el vicerrector
 * @param programas conteos por programa
 */
public record PendientesFormularioResponse(
    PeriodoResumenResponse periodo,
    int itemsCoevaluacion,
    long pendientes,
    List<DocenteAvance> docentes,
    List<ProgramaAvance> programas) {

  public record DocenteAvance(
      Long idDocente, String nombreDocente, String programa, String estado, int respondidos) {}

  public record ProgramaAvance(
      String programa,
      String facultad,
      long docentes,
      long sinIniciar,
      long borrador,
      long enviadas,
      long autoevaluacionesEnviadas) {}
}
