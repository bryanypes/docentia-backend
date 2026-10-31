package com.uniautonoma.docentia.service.analisis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.uniautonoma.docentia.config.IaProperties;
import com.uniautonoma.docentia.service.analisis.ModeloIA.ComentarioAnonimo;
import com.uniautonoma.docentia.service.analisis.ModeloIA.ModeloIAException;
import com.uniautonoma.docentia.service.analisis.ModeloIA.PeticionAnalisis;
import com.uniautonoma.docentia.service.analisis.ModeloIA.RespuestaModelo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ModeloIAChatCompletionsTest {

  private static final IaProperties PROPIEDADES =
      new IaProperties(
          true,
          "chat-completions",
          "https://proveedor.prueba/v1",
          "modelo-de-prueba",
          30,
          "clave-de-prueba",
          10,
          new IaProperties.Worker(false, 5000));
  private static final PeticionAnalisis PETICION =
      new PeticionAnalisis(
          "Instrucción versionada", List.of(new ComentarioAnonimo(7L, "Explica muy bien")));

  private MockRestServiceServer proveedor;
  private ModeloIAChatCompletions modelo;

  @BeforeEach
  void conectar() {
    RestClient.Builder builder = RestClient.builder();
    proveedor = MockRestServiceServer.bindTo(builder).build();
    modelo = new ModeloIAChatCompletions(builder, PROPIEDADES);
  }

  @Test
  void enviaInstruccionComentariosModeloYClaveYDevuelveElContenido() {
    proveedor
        .expect(requestTo("https://proveedor.prueba/v1/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer clave-de-prueba"))
        .andExpect(jsonPath("$.model").value("modelo-de-prueba"))
        .andExpect(jsonPath("$.response_format.type").value("json_schema"))
        .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
        .andExpect(jsonPath("$.messages[0].content").value("Instrucción versionada"))
        .andExpect(
            jsonPath("$.messages[1].content")
                .value("{\"comentarios\":[{\"id\":7,\"texto\":\"Explica muy bien\"}]}"))
        .andRespond(
            withSuccess(
                """
                {"model":"modelo-de-prueba-2026","choices":[{"message":{"content":"{\\"hallazgos\\":[]}"}}]}
                """,
                MediaType.APPLICATION_JSON));

    RespuestaModelo respuesta = modelo.analizar(PETICION);

    assertThat(respuesta.contenido()).isEqualTo("{\"hallazgos\":[]}");
    assertThat(respuesta.modelo()).isEqualTo("modelo-de-prueba-2026");
    proveedor.verify();
  }

  @Test
  void unErrorDelProveedorSeConvierteEnModeloIAException() {
    proveedor
        .expect(requestTo("https://proveedor.prueba/v1/chat/completions"))
        .andRespond(withServerError());

    assertThatThrownBy(() -> modelo.analizar(PETICION)).isInstanceOf(ModeloIAException.class);
  }

  @Test
  void sinClaveNoArranca() {
    IaProperties sinClave =
        new IaProperties(
            true,
            "chat-completions",
            "https://x",
            "m",
            30,
            "",
            10,
            new IaProperties.Worker(false, 1));

    assertThatThrownBy(() -> new ModeloIAChatCompletions(RestClient.builder(), sinClave))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DOCENTIA_IA_CLAVE");
  }
}
