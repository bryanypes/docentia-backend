package com.uniautonoma.docentia.service.analisis;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniautonoma.docentia.entity.TipoHallazgo;
import com.uniautonoma.docentia.service.analisis.LectorRespuestaAnalisis.CitaPropuesta;
import com.uniautonoma.docentia.service.analisis.LectorRespuestaAnalisis.HallazgoPropuesto;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Los comentarios que dicen lo mismo quedan en un solo hallazgo, y cada uno una sola vez. */
class AgrupadorHallazgosTest {

  private static HallazgoPropuesto fortaleza(String criterio, String texto, Long... ids) {
    return new HallazgoPropuesto(
        TipoHallazgo.FORTALEZA,
        texto,
        Arrays.stream(ids).map(id -> new CitaPropuesta(id, null)).toList(),
        criterio);
  }

  private static List<Long> ids(HallazgoPropuesto hallazgo) {
    return hallazgo.citas().stream().map(CitaPropuesta::comentarioId).toList();
  }

  @Test
  void funde_los_hallazgos_del_mismo_criterio_y_tipo() {
    List<HallazgoPropuesto> agrupados =
        AgrupadorHallazgos.agrupar(
            List.of(
                fortaleza("C2", "Explica con claridad", 1L, 2L),
                fortaleza("C4", "Trato paciente", 3L),
                fortaleza("C2", "Los temas se entienden", 4L)));

    assertThat(agrupados).hasSize(2);
    assertThat(agrupados.get(0).criterio()).isEqualTo("C2");
    assertThat(agrupados.get(0).texto()).isEqualTo("Explica con claridad. Los temas se entienden");
    assertThat(ids(agrupados.get(0))).containsExactly(1L, 2L, 4L);
    assertThat(agrupados.get(1).criterio()).isEqualTo("C4");
  }

  @Test
  void cada_comentario_queda_en_el_hallazgo_mas_respaldado() {
    List<HallazgoPropuesto> agrupados =
        AgrupadorHallazgos.agrupar(
            List.of(
                fortaleza("C4", "Trato paciente", 5L),
                fortaleza("C2", "Explica con claridad", 5L, 6L, 7L)));

    assertThat(agrupados).hasSize(1);
    assertThat(agrupados.get(0).criterio()).isEqualTo("C2");
    assertThat(ids(agrupados.get(0))).containsExactly(5L, 6L, 7L);
  }

  @Test
  void conserva_el_orden_del_modelo_y_los_hallazgos_sin_criterio() {
    HallazgoPropuesto oportunidad =
        new HallazgoPropuesto(
            TipoHallazgo.OPORTUNIDAD_DE_MEJORA,
            "Publicar notas a tiempo",
            List.of(new CitaPropuesta(8L, null)),
            null);
    List<HallazgoPropuesto> agrupados =
        AgrupadorHallazgos.agrupar(List.of(oportunidad, fortaleza(null, "Es muy querido")));

    assertThat(agrupados)
        .extracting(HallazgoPropuesto::texto)
        .containsExactly("Publicar notas a tiempo", "Es muy querido");
  }
}
