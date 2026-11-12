package com.uniautonoma.docentia.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Revisiones del perfil prod al arrancar. */
@ExtendWith(OutputCaptureExtension.class)
class ValidacionProduccionTest {

  private static final String URL = "https://nova.uniautonoma.edu.co/Evadocente-app";
  private static final String SECRETO = "un-secreto-propio-de-mas-de-treinta-y-dos-caracteres";

  @Test
  void unaUrlDeAplicacionEnLocalhostOVaciaDetieneElArranque() {
    for (String url : new String[] {"http://localhost:5173", "", "http://127.0.0.1:8080"}) {
      assertThatThrownBy(
              () -> new ValidacionProduccion(SECRETO, new GoogleProperties("cliente"), url))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("DOCENTIA_APP_URL");
    }
  }

  @Test
  void conLaUrlPublicaArrancaSinErrores() {
    new ValidacionProduccion(
        SECRETO, new GoogleProperties("cliente"), "https://nova.uniautonoma.edu.co/Evadocente-app");
  }

  @Test
  void unSecretoDeDesarrolloDetieneElArranque() {
    assertThatThrownBy(
            () ->
                new ValidacionProduccion(
                    "secreto-de-desarrollo-local-no-usar-en-produccion",
                    new GoogleProperties("cliente"),
                    URL))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DOCENTIA_JWT_SECRETO");
  }

  @Test
  void sinGoogleAvisaEnElLogQueLosEstudiantesNoPodranEntrar(CapturedOutput salida) {
    new ValidacionProduccion(SECRETO, new GoogleProperties(""), URL);

    assertThat(salida.getAll())
        .contains("Falta DOCENTIA_GOOGLE_CLIENT_ID")
        .contains("los estudiantes no podrán iniciar sesión")
        .contains("VITE_GOOGLE_CLIENT_ID");
  }

  @Test
  void conGoogleNoAvisa(CapturedOutput salida) {
    new ValidacionProduccion(
        SECRETO, new GoogleProperties("cliente.apps.googleusercontent.com"), URL);

    assertThat(salida.getAll()).doesNotContain("DOCENTIA_GOOGLE_CLIENT_ID");
    assertThat(ValidacionProduccion.avisoGoogle(new GoogleProperties(" "))).isPresent();
    assertThat(ValidacionProduccion.avisoGoogle(null)).isPresent();
  }
}
