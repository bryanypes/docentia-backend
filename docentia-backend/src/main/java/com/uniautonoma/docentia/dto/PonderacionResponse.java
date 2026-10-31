package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.PesosFuentes;
import java.math.BigDecimal;

public record PonderacionResponse(
    PeriodoResumenResponse periodo,
    BigDecimal pesoEstudiantes,
    BigDecimal pesoCoordinacion,
    BigDecimal pesoVicerrectoria,
    boolean esPredeterminada) {

  public static PonderacionResponse desde(
      PeriodoAcademico periodo, PesosFuentes pesos, boolean esPredeterminada) {
    return new PonderacionResponse(
        PeriodoResumenResponse.desde(periodo),
        pesos.pesoEstudiantes(),
        pesos.pesoCoordinacion(),
        pesos.pesoVicerrectoria(),
        esPredeterminada);
  }
}
