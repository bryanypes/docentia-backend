package com.uniautonoma.docentia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** Porcentaje con el que cada fuente de evaluación entra al promedio ponderado. */
@Embeddable
public record PesosFuentes(
    @Column(nullable = false, precision = 5, scale = 2) BigDecimal pesoEstudiantes,
    @Column(nullable = false, precision = 5, scale = 2) BigDecimal pesoCoordinacion,
    @Column(nullable = false, precision = 5, scale = 2) BigDecimal pesoVicerrectoria) {

  public static final BigDecimal PESO_ESTUDIANTES = new BigDecimal("50");
  public static final BigDecimal PESO_COORDINACION = new BigDecimal("30");
  public static final BigDecimal PESO_VICERRECTORIA = new BigDecimal("20");
  public static final BigDecimal TOTAL_PORCENTAJE = new BigDecimal("100");

  public static PesosFuentes predeterminados() {
    return new PesosFuentes(PESO_ESTUDIANTES, PESO_COORDINACION, PESO_VICERRECTORIA);
  }

  public boolean sumanCien() {
    return pesoEstudiantes.add(pesoCoordinacion).add(pesoVicerrectoria).compareTo(TOTAL_PORCENTAJE)
        == 0;
  }

  public BigDecimal pesoDe(FuenteEvaluacion fuente) {
    return switch (fuente) {
      case ESTUDIANTES -> pesoEstudiantes;
      case COORDINACION -> pesoCoordinacion;
      case VICERRECTORIA -> pesoVicerrectoria;
    };
  }
}
