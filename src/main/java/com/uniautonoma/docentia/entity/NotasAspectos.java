package com.uniautonoma.docentia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** Los cinco aspectos que se califican de 1.0 a 5.0 en cada fuente de evaluación. */
@Embeddable
public record NotasAspectos(
    @Column(nullable = false, precision = 3, scale = 2) BigDecimal dominioDisciplinar,
    @Column(nullable = false, precision = 3, scale = 2) BigDecimal planeacionAcademica,
    @Column(nullable = false, precision = 3, scale = 2) BigDecimal metodologiaEnsenanza,
    @Column(nullable = false, precision = 3, scale = 2) BigDecimal evaluacionAprendizaje,
    @Column(nullable = false, precision = 3, scale = 2) BigDecimal compromisoInstitucional) {

  private static final BigDecimal CANTIDAD_ASPECTOS = BigDecimal.valueOf(5);

  /**
   * Nota de la fuente: promedio simple de los cinco aspectos. No se redondea aquí; dividir entre 5
   * siempre da un decimal exacto y el redondeo se deja para el promedio ponderado final.
   */
  public BigDecimal promedio() {
    return dominioDisciplinar
        .add(planeacionAcademica)
        .add(metodologiaEnsenanza)
        .add(evaluacionAprendizaje)
        .add(compromisoInstitucional)
        .divide(CANTIDAD_ASPECTOS);
  }
}
