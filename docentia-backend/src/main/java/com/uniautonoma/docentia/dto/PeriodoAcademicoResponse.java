package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.PeriodoAcademico;
import java.time.LocalDate;
import java.util.List;

public record PeriodoAcademicoResponse(
    Long idPeriodo,
    String nombre,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    boolean esActivo,
    List<EtapaPeriodoResponse> etapas) {

  public static PeriodoAcademicoResponse desde(PeriodoAcademico periodo) {
    return new PeriodoAcademicoResponse(
        periodo.getIdPeriodo(),
        periodo.getNombre(),
        periodo.getFechaInicio(),
        periodo.getFechaFin(),
        periodo.getEsActivo(),
        periodo.getEtapas().stream().map(EtapaPeriodoResponse::desde).toList());
  }
}
