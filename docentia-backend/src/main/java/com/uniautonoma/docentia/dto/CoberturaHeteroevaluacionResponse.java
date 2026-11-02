package com.uniautonoma.docentia.dto;

import java.math.BigDecimal;
import java.util.List;

/** Participación sin identificar estudiantes ni mostrar puntajes. */
public record CoberturaHeteroevaluacionResponse(
    PeriodoResumenResponse periodo,
    Fila total,
    List<Fila> programas,
    PaginaResponse<Fila> docentes) {

  /**
   * @param nombre docente o programa; "Total" en el total
   */
  public record Fila(
      Long idDocente,
      String nombre,
      String programa,
      long matriculados,
      long respondieron,
      BigDecimal porcentaje) {}
}
