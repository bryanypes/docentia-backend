package com.uniautonoma.docentia.service.analisis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.uniautonoma.docentia.entity.TipoHallazgo;
import com.uniautonoma.docentia.service.analisis.LectorRespuestaAnalisis.CitaPropuesta;
import com.uniautonoma.docentia.service.analisis.LectorRespuestaAnalisis.HallazgoPropuesto;
import com.uniautonoma.docentia.service.analisis.LectorRespuestaAnalisis.RespuestaInvalidaException;
import java.util.List;
import org.junit.jupiter.api.Test;

class LectorRespuestaAnalisisTest {

  private final LectorRespuestaAnalisis lector = new LectorRespuestaAnalisis();

  @Test
  void leeLosHallazgosConSusCitas() {
    List<HallazgoPropuesto> hallazgos =
        lector.leer(
            """
            {"hallazgos":[{"tipo":"fortaleza","texto":"Explica con claridad",
              "citas":[{"comentarioId":12,"fragmento":"explica muy claro"}]},
             {"tipo":"oportunidad_de_mejora","texto":"Publicar notas a tiempo",
              "citas":[{"comentarioId":"13","fragmento":"las notas tardan"}]}]}
            """);

    assertThat(hallazgos)
        .containsExactly(
            new HallazgoPropuesto(
                TipoHallazgo.FORTALEZA,
                "Explica con claridad",
                List.of(new CitaPropuesta(12L, "explica muy claro"))),
            new HallazgoPropuesto(
                TipoHallazgo.OPORTUNIDAD_DE_MEJORA,
                "Publicar notas a tiempo",
                List.of(new CitaPropuesta(13L, "las notas tardan"))));
  }

  @Test
  void aceptaElJsonEnvueltoEnUnBloqueDeCodigoYUnaListaVacia() {
    assertThat(lector.leer("```json\n{\"hallazgos\": []}\n```")).isEmpty();
  }

  @Test
  void unaCitaSinIdentificadorNumericoQuedaSinComentario() {
    assertThat(
            lector
                .leer(
                    "{\"hallazgos\":[{\"tipo\":\"fortaleza\",\"texto\":\"x\","
                        + "\"citas\":[{\"comentarioId\":\"C3\",\"fragmento\":\"y\"}]}]}")
                .get(0)
                .citas())
        .containsExactly(new CitaPropuesta(null, "y"));
  }

  @Test
  void rechazaLoQueNoTieneElFormatoPedido() {
    List<String> invalidos =
        List.of(
            "Aquí tienes el análisis: fortalezas…",
            "[]",
            "{\"resultado\": []}",
            "{\"hallazgos\":[{\"tipo\":\"debilidad\",\"texto\":\"x\",\"citas\":[]}]}",
            "{\"hallazgos\":[{\"tipo\":\"fortaleza\",\"texto\":\"  \",\"citas\":[]}]}",
            "{\"hallazgos\":[{\"tipo\":\"fortaleza\",\"texto\":\"x\"}]}",
            "{\"hallazgos\":[{\"tipo\":\"fortaleza\",\"texto\":\"x\",\"citas\":[{\"comentarioId\":1}]}]}");

    for (String invalido : invalidos) {
      assertThatThrownBy(() -> lector.leer(invalido))
          .as(invalido)
          .isInstanceOf(RespuestaInvalidaException.class);
    }
  }
}
