package com.uniautonoma.docentia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.uniautonoma.docentia.entity.CategoriaDesempeno;
import com.uniautonoma.docentia.exception.DatosInvalidosException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Límites de la escala de la Resolución 074. */
class ClasificadorDesempenoTest {

  @ParameterizedTest
  @CsvSource({
    "5.00, MUY_ALTO",
    "4.60, MUY_ALTO",
    "4.59, ALTO",
    "3.80, ALTO",
    "3.79, ACEPTABLE",
    "3.00, ACEPTABLE",
    "2.99, BAJO",
    "2.20, BAJO",
    "2.19, MUY_BAJO",
    "1.00, MUY_BAJO",
    "0.00, MUY_BAJO",
  })
  void respetaLosLimitesDeCadaCategoria(String nota, CategoriaDesempeno esperada) {
    assertThat(ClasificadorDesempeno.clasificar(new BigDecimal(nota))).isEqualTo(esperada);
  }

  @Test
  void redondeaADosDecimalesAntesDeClasificar() {
    assertThat(ClasificadorDesempeno.clasificar(new BigDecimal("4.595")))
        .isEqualTo(CategoriaDesempeno.MUY_ALTO);
    assertThat(ClasificadorDesempeno.clasificar(new BigDecimal("4.594")))
        .isEqualTo(CategoriaDesempeno.ALTO);
  }

  @ParameterizedTest
  @ValueSource(strings = {"-0.01", "5.01"})
  void fueraDeLaEscalaEsUnErrorControlado(String nota) {
    assertThatThrownBy(() -> ClasificadorDesempeno.clasificar(new BigDecimal(nota)))
        .isInstanceOf(DatosInvalidosException.class);
  }

  @Test
  void sinNotaEsUnErrorControlado() {
    assertThatThrownBy(() -> ClasificadorDesempeno.clasificar(null))
        .isInstanceOf(DatosInvalidosException.class);
  }
}
