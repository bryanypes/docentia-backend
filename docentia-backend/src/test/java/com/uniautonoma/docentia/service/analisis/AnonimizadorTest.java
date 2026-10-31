package com.uniautonoma.docentia.service.analisis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnonimizadorTest {

  private final Anonimizador anonimizador = new Anonimizador();

  @Test
  void quitaCorreosEnlacesCodigosNumerosLargosYUsuarios() {
    String texto =
        "Escríbanle a pedro.perez@uniautonoma.edu.co o a @pedrito, mi código es P000017482,"
            + " cédula 1.061.234.567, celular 300 123 4567, ver https://x.co/a";

    assertThat(anonimizador.anonimizar(texto))
        .isEqualTo(
            "Escríbanle a [correo] o a [usuario], mi código es [código],"
                + " cédula [número], celular [número], ver [enlace]");
  }

  @Test
  void quitaElNombreCuandoElEstudianteSePresenta() {
    assertThat(anonimizador.anonimizar("Me llamo Laura Gómez y la clase es buena"))
        .isEqualTo("Me llamo [nombre] y la clase es buena");
  }

  @Test
  void noTocaElContenidoSobreLaPracticaDocenteNiNumerosCortos() {
    String texto = "Explica bien, pero en el corte 2 tardó 15 días en publicar notas";

    assertThat(anonimizador.anonimizar(texto)).isEqualTo(texto);
  }
}
