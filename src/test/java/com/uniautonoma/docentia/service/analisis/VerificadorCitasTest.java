package com.uniautonoma.docentia.service.analisis;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniautonoma.docentia.service.analisis.ModeloIA.ComentarioAnonimo;
import java.util.List;
import org.junit.jupiter.api.Test;

class VerificadorCitasTest {

  private final VerificadorCitas verificador =
      new VerificadorCitas(
          List.of(
              new ComentarioAnonimo(
                  1L, "Las notas de los parciales se demoran más de dos semanas."),
              new ComentarioAnonimo(2L, "Me llamo [nombre], explica con ejemplos reales.")));

  @Test
  void aceptaElFragmentoAunqueCambienMayusculasTildesYPuntuacion() {
    assertThat(verificador.esVerificable(1L, "las notas de los parciales se demoran")).isTrue();
    assertThat(verificador.esVerificable(1L, "SE DEMORAN MAS de dos semanas")).isTrue();
    assertThat(verificador.esVerificable(1L, "parciales, se demoran...")).isTrue();
    assertThat(verificador.esVerificable(2L, "Me llamo [nombre], explica")).isTrue();
  }

  @Test
  void rechazaCitasInventadasParafraseadasOAjenas() {
    assertThat(verificador.esVerificable(1L, "las notas nunca se publican")).isFalse();
    assertThat(verificador.esVerificable(1L, "las calificaciones tardan dos semanas")).isFalse();
    // El fragmento existe, pero en otro comentario.
    assertThat(verificador.esVerificable(2L, "se demoran más de dos semanas")).isFalse();
    // Comentario que no se envió (filtrado, de otro docente o inventado).
    assertThat(verificador.esVerificable(99L, "explica con ejemplos reales")).isFalse();
    assertThat(verificador.esVerificable(null, "explica con ejemplos reales")).isFalse();
  }

  @Test
  void unaSolaPalabraOUnPedazoDePalabraNoCuentanComoCita() {
    assertThat(verificador.esVerificable(1L, "notas")).isFalse();
    assertThat(verificador.esVerificable(1L, "otas de los parcia")).isFalse();
  }
}
