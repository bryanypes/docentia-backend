package com.uniautonoma.docentia.service.analisis;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniautonoma.docentia.service.analisis.ModeloIA.ComentarioAnonimo;
import com.uniautonoma.docentia.service.analisis.ModeloIA.PeticionAnalisis;
import java.util.List;
import org.junit.jupiter.api.Test;

class HuellaEntradaTest {

  private static final PeticionAnalisis PETICION =
      new PeticionAnalisis(
          "instrucción",
          List.of(
              new ComentarioAnonimo(1L, "Explica muy bien los temas"),
              new ComentarioAnonimo(2L, "Las notas se demoran mucho")),
          List.of("D05+4"));

  private static String huella(String modelo, String version, String topes, PeticionAnalisis p) {
    return ProcesadorSolicitudesAnalisis.huella(modelo, version, topes, p);
  }

  @Test
  void cambiaConElPromptElModeloLosTopesYLaEntrada() {
    String base = huella("chat:m", "analisis-v3", "150|300", PETICION);

    assertThat(huella("chat:m", "analisis-v3", "150|300", PETICION)).isEqualTo(base);
    assertThat(huella("chat:m", "analisis-v4", "150|300", PETICION)).isNotEqualTo(base);
    assertThat(huella("chat:otro", "analisis-v3", "150|300", PETICION)).isNotEqualTo(base);
    assertThat(huella("chat:m", "analisis-v3", "100|300", PETICION)).isNotEqualTo(base);
    PeticionAnalisis otra =
        new PeticionAnalisis(
            "instrucción",
            List.of(new ComentarioAnonimo(1L, "Explica muy bien los temas")),
            List.of("D05+4"));
    assertThat(huella("chat:m", "analisis-v3", "150|300", otra)).isNotEqualTo(base);
  }

  @Test
  void esUnaHuellaQueNoContieneLosComentarios() {
    String huella = huella("chat:m", "analisis-v3", "150|300", PETICION);

    assertThat(huella).matches("[0-9a-f]{64}").doesNotContain("Explica").doesNotContain("notas");
  }
}
