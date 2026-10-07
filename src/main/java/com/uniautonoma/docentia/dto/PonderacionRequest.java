package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.PesosFuentes;
import java.math.BigDecimal;

public record PonderacionRequest(
    @PorcentajePeso BigDecimal pesoEstudiantes,
    @PorcentajePeso BigDecimal pesoCoordinacion,
    @PorcentajePeso BigDecimal pesoVicerrectoria) {

  public PesosFuentes aPesosFuentes() {
    return new PesosFuentes(pesoEstudiantes, pesoCoordinacion, pesoVicerrectoria);
  }
}
