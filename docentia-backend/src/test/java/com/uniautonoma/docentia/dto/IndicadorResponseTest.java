package com.uniautonoma.docentia.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class IndicadorResponseTest {

  @Test
  void laVariacionEsLaDiferenciaConElPeriodoAnterior() {
    IndicadorResponse indicador =
        IndicadorResponse.comparar(new BigDecimal("3.89"), new BigDecimal("4.00"));

    assertThat(indicador.variacion()).isEqualByComparingTo("-0.11");
  }

  @Test
  void sinDatoAnteriorOActualLaVariacionEsNulaYNoCero() {
    assertThat(IndicadorResponse.comparar(BigDecimal.TEN, null).variacion()).isNull();
    assertThat(IndicadorResponse.comparar(null, BigDecimal.TEN).variacion()).isNull();
    assertThat(IndicadorResponse.comparar(BigDecimal.ZERO, BigDecimal.ZERO).variacion())
        .isEqualByComparingTo("0");
  }
}
