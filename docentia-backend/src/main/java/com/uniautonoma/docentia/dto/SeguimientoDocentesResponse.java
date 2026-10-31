package com.uniautonoma.docentia.dto;

import java.util.List;

/**
 * Docentes del alcance con su promedio del periodo activo.
 *
 * @param periodo periodo activo, o nulo si no hay
 * @param docentes en orden por nombre; sin periodo activo el promedio de cada uno es nulo
 */
public record SeguimientoDocentesResponse(
    PeriodoResumenResponse periodo, List<DocenteConPromedio> docentes) {

  public record DocenteConPromedio(DocenteResponse docente, PromedioPonderadoResponse promedio) {}
}
