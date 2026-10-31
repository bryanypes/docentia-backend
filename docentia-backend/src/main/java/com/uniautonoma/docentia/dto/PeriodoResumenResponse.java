package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.PeriodoAcademico;

public record PeriodoResumenResponse(Long idPeriodo, String nombre) {

  public static PeriodoResumenResponse desde(PeriodoAcademico periodo) {
    return new PeriodoResumenResponse(periodo.getIdPeriodo(), periodo.getNombre());
  }
}
