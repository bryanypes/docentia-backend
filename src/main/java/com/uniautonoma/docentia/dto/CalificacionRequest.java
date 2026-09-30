package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.NotasAspectos;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CalificacionRequest(
    @NotaAspecto BigDecimal dominioDisciplinar,
    @NotaAspecto BigDecimal planeacionAcademica,
    @NotaAspecto BigDecimal metodologiaEnsenanza,
    @NotaAspecto BigDecimal evaluacionAprendizaje,
    @NotaAspecto BigDecimal compromisoInstitucional,
    @Size(max = 1000, message = "La observación no puede superar los 1000 caracteres")
        String observacion) {

  public NotasAspectos aNotasAspectos() {
    return new NotasAspectos(
        dominioDisciplinar,
        planeacionAcademica,
        metodologiaEnsenanza,
        evaluacionAprendizaje,
        compromisoInstitucional);
  }
}
