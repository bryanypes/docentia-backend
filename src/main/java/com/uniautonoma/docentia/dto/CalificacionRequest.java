package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.NotasAspectos;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Notas de los cinco aspectos que da una fuente.
 *
 * @param motivoCorreccion opcional; si la calificación ya existía y sus notas cambian, queda en el
 *     historial de correcciones
 */
public record CalificacionRequest(
    @NotaAspecto BigDecimal dominioDisciplinar,
    @NotaAspecto BigDecimal planeacionAcademica,
    @NotaAspecto BigDecimal metodologiaEnsenanza,
    @NotaAspecto BigDecimal evaluacionAprendizaje,
    @NotaAspecto BigDecimal compromisoInstitucional,
    @Size(max = 1000, message = "La observación no puede superar los 1000 caracteres")
        String observacion,
    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
        String motivoCorreccion) {

  public CalificacionRequest(
      BigDecimal dominioDisciplinar,
      BigDecimal planeacionAcademica,
      BigDecimal metodologiaEnsenanza,
      BigDecimal evaluacionAprendizaje,
      BigDecimal compromisoInstitucional,
      String observacion) {
    this(
        dominioDisciplinar,
        planeacionAcademica,
        metodologiaEnsenanza,
        evaluacionAprendizaje,
        compromisoInstitucional,
        observacion,
        null);
  }

  public NotasAspectos aNotasAspectos() {
    return new NotasAspectos(
        dominioDisciplinar,
        planeacionAcademica,
        metodologiaEnsenanza,
        evaluacionAprendizaje,
        compromisoInstitucional);
  }
}
