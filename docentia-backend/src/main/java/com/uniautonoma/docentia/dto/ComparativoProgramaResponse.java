package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import java.math.BigDecimal;
import java.util.List;

/** Notas por fuente del docente frente al promedio real de su programa. */
public record ComparativoProgramaResponse(
    PeriodoResumenResponse periodo, String nombrePrograma, List<FuenteComparada> fuentes) {

  public record FuenteComparada(
      FuenteEvaluacion fuente, BigDecimal notaDocente, BigDecimal promedioPrograma) {}
}
