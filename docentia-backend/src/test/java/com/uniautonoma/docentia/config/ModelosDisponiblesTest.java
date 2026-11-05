package com.uniautonoma.docentia.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniautonoma.docentia.service.analisis.ModeloIAChatCompletions;
import com.uniautonoma.docentia.service.analisis.ModelosDisponibles;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorLocal;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorLocal.ParametrosAnalisis;
import com.uniautonoma.docentia.service.analisis.local.Lexico;
import com.uniautonoma.docentia.service.analisis.local.ModeloIALocal;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class ModelosDisponiblesTest {

  private static final AhorroIaProperties AHORRO =
      new AhorroIaProperties(150, 300, 16000, 4000, 500000, 3000000, 60, 1, null, null);
  private static final ModeloIALocal LOCAL =
      new ModeloIALocal(
          new AnalizadorLocal(
              Lexico.cargar("ia/lexico-v1.txt"), new ParametrosAnalisis(3, 0.2, 5, 10, 5)));

  private static IaProperties propiedades(String url, String modelo, String clave) {
    return new IaProperties(
        true, "local", url, modelo, 60, clave, 10, new IaProperties.Worker(false, 5000), true);
  }

  @Test
  void conElLocalPorDefectoYElProveedorCompletoElExternoQuedaParaElegir() {
    ModelosDisponibles modelos =
        new AnalisisIaConfig()
            .modelosDisponibles(
                LOCAL,
                RestClient.builder(),
                propiedades("https://proveedor.prueba/v1", "modelo-x", "clave-x"),
                AHORRO);

    assertThat(modelos.codigoPorDefecto()).isEqualTo("LOCAL");
    assertThat(modelos.porDefecto()).isSameAs(LOCAL);
    assertThat(modelos.externo()).get().isInstanceOf(ModeloIAChatCompletions.class);
    assertThat(modelos.nombreExterno()).isEqualTo("modelo-x");
  }

  @Test
  void sinUrlModeloOClaveNoHayExterno() {
    for (IaProperties incompleta :
        new IaProperties[] {
          propiedades(null, "m", "c"),
          propiedades("https://x", null, "c"),
          propiedades("https://x", "m", "")
        }) {
      ModelosDisponibles modelos =
          new AnalisisIaConfig()
              .modelosDisponibles(LOCAL, RestClient.builder(), incompleta, AHORRO);
      assertThat(modelos.hayExterno()).isFalse();
      assertThat(modelos.nombreExterno()).isNull();
    }
  }
}
