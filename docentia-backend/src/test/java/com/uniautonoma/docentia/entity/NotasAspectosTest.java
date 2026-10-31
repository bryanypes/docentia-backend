package com.uniautonoma.docentia.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NotasAspectosTest {

  @Test
  void lasMismasNotasConDistintaEscalaSonIguales() {
    assertThat(iguales("4.0").mismasNotas(iguales("4.00"))).isTrue();
  }

  @Test
  void unSoloAspectoDistintoBastaParaQueSeanDiferentes() {
    NotasAspectos base = iguales("4.0");
    NotasAspectos otra =
        new NotasAspectos(
            new BigDecimal("4.0"),
            new BigDecimal("4.0"),
            new BigDecimal("4.0"),
            new BigDecimal("4.0"),
            new BigDecimal("4.1"));

    assertThat(base.mismasNotas(otra)).isFalse();
    assertThat(base.mismasNotas(null)).isFalse();
  }

  @Test
  void laNotaDeLaFuenteEsElPromedioExactoDeLosCincoAspectos() {
    NotasAspectos notas =
        new NotasAspectos(
            new BigDecimal("4.33"),
            new BigDecimal("4.00"),
            new BigDecimal("3.50"),
            new BigDecimal("5.00"),
            new BigDecimal("4.00"));

    assertThat(notas.promedio()).isEqualByComparingTo("4.166");
  }

  private NotasAspectos iguales(String nota) {
    BigDecimal valor = new BigDecimal(nota);
    return new NotasAspectos(valor, valor, valor, valor, valor);
  }
}
