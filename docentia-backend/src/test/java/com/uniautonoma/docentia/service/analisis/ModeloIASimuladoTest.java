package com.uniautonoma.docentia.service.analisis;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniautonoma.docentia.entity.TipoHallazgo;
import com.uniautonoma.docentia.service.analisis.LectorRespuestaAnalisis.HallazgoPropuesto;
import com.uniautonoma.docentia.service.analisis.ModeloIA.ComentarioAnonimo;
import com.uniautonoma.docentia.service.analisis.ModeloIA.PeticionAnalisis;
import com.uniautonoma.docentia.service.analisis.ModeloIA.RespuestaModelo;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModeloIASimuladoTest {

  private final ModeloIASimulado modelo = new ModeloIASimulado();
  private final List<ComentarioAnonimo> comentarios =
      List.of(
          new ComentarioAnonimo(1L, "Explica con ejemplos reales. Muy buena clase"),
          new ComentarioAnonimo(2L, "Las notas se demoran mucho en publicarse"),
          new ComentarioAnonimo(3L, "Domina el tema"),
          new ComentarioAnonimo(4L, "Nunca responde los correos"));

  @Test
  void paraLosMismosComentariosRespondeSiempreLoMismo() {
    PeticionAnalisis peticion = new PeticionAnalisis("instrucción", comentarios);

    RespuestaModelo primera = modelo.analizar(peticion);

    assertThat(modelo.analizar(peticion)).isEqualTo(primera);
    assertThat(primera.modelo()).isEqualTo(ModeloIASimulado.NOMBRE);
  }

  @Test
  void produceJsonValidoConCitasCopiadasDeLosComentarios() {
    List<HallazgoPropuesto> hallazgos =
        new LectorRespuestaAnalisis()
            .leer(modelo.analizar(new PeticionAnalisis("x", comentarios)).contenido());

    assertThat(hallazgos)
        .extracting(HallazgoPropuesto::tipo)
        .containsExactly(
            TipoHallazgo.FORTALEZA,
            TipoHallazgo.OPORTUNIDAD_DE_MEJORA,
            TipoHallazgo.FORTALEZA,
            TipoHallazgo.OPORTUNIDAD_DE_MEJORA);
    assertThat(hallazgos.get(0).citas().get(0).fragmento())
        .isEqualTo("Explica con ejemplos reales");
    for (HallazgoPropuesto hallazgo : hallazgos) {
      var cita = hallazgo.citas().get(0);
      String original =
          comentarios.stream()
              .filter(comentario -> comentario.id().equals(cita.comentarioId()))
              .findFirst()
              .orElseThrow()
              .texto();
      assertThat(original).contains(cita.fragmento());
    }
  }
}
