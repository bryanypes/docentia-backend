package com.uniautonoma.docentia.service.analisis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uniautonoma.docentia.config.IaProperties;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente para proveedores que exponen el formato chat/completions, que comparten muchos servicios
 * comerciales y servidores locales. El proveedor concreto, el modelo y la clave salen de la
 * configuración; para un proveedor con otro formato se agrega otra implementación de {@link
 * ModeloIA}.
 */
public class ModeloIAChatCompletions implements ModeloIA {

  static final String RUTA = "/chat/completions";

  private final RestClient restClient;
  private final String modelo;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * @param restClientBuilder builder con los tiempos de espera ya configurados
   * @throws IllegalStateException si falta la dirección, el modelo o la clave
   */
  public ModeloIAChatCompletions(RestClient.Builder restClientBuilder, IaProperties propiedades) {
    exigir(propiedades.urlBase(), "docentia.ia.url-base");
    exigir(propiedades.modelo(), "docentia.ia.modelo");
    exigir(propiedades.clave(), "docentia.ia.clave (variable DOCENTIA_IA_CLAVE)");
    this.modelo = propiedades.modelo();
    this.restClient =
        restClientBuilder
            .baseUrl(propiedades.urlBase())
            .defaultHeaders(cabeceras -> cabeceras.setBearerAuth(propiedades.clave()))
            .build();
  }

  @Override
  public RespuestaModelo analizar(PeticionAnalisis peticion) {
    try {
      JsonNode respuesta =
          restClient
              .post()
              .uri(RUTA)
              .contentType(MediaType.APPLICATION_JSON)
              .body(cuerpo(peticion))
              .retrieve()
              .body(JsonNode.class);
      JsonNode contenido = respuesta == null ? null : respuesta.at("/choices/0/message/content");
      if (contenido == null || !contenido.isTextual()) {
        throw new ModeloIAException("El proveedor respondió sin contenido", null);
      }
      String modeloUsado = respuesta.path("model").asText(modelo);
      return new RespuestaModelo(contenido.asText(), modeloUsado);
    } catch (RestClientException excepcion) {
      throw new ModeloIAException(
          "Falló la llamada al proveedor: " + excepcion.getMessage(), excepcion);
    }
  }

  private String cuerpo(PeticionAnalisis peticion) {
    ObjectNode cuerpo = objectMapper.createObjectNode();
    cuerpo.put("model", modelo);
    cuerpo.put("temperature", 0);
    cuerpo.put("max_tokens", 4096);
    cuerpo.set("response_format", formatoJson());
    ArrayNode mensajes = cuerpo.putArray("messages");
    mensajes.addObject().put("role", "system").put("content", peticion.instruccion());
    ObjectNode entrada = objectMapper.createObjectNode();
    ArrayNode comentarios = entrada.putArray("comentarios");
    peticion
        .comentarios()
        .forEach(
            comentario ->
                comentarios
                    .addObject()
                    .put("id", comentario.id())
                    .put("texto", comentario.texto()));
    try {
      mensajes
          .addObject()
          .put("role", "user")
          .put("content", objectMapper.writeValueAsString(entrada));
      return objectMapper.writeValueAsString(cuerpo);
    } catch (JsonProcessingException excepcion) {
      throw new ModeloIAException("No se pudo armar la petición", excepcion);
    }
  }

  /**
   * Claude rechaza {@code json_object} en su capa compatible con chat/completions. {@code json_schema}
   * con {@code strict} lo acepta y también los proveedores que hablan el mismo formato.
   */
  private ObjectNode formatoJson() {
    ObjectNode cita = objectMapper.createObjectNode();
    cita.put("type", "object");
    cita.put("additionalProperties", false);
    ObjectNode propiedadesCita = cita.putObject("properties");
    propiedadesCita.putObject("comentarioId").put("type", "integer");
    propiedadesCita.putObject("fragmento").put("type", "string");
    cita.putArray("required").add("comentarioId").add("fragmento");

    ObjectNode hallazgo = objectMapper.createObjectNode();
    hallazgo.put("type", "object");
    hallazgo.put("additionalProperties", false);
    ObjectNode propiedadesHallazgo = hallazgo.putObject("properties");
    propiedadesHallazgo
        .putObject("tipo")
        .put("type", "string")
        .putArray("enum")
        .add("fortaleza")
        .add("oportunidad_de_mejora");
    propiedadesHallazgo.putObject("texto").put("type", "string");
    propiedadesHallazgo.putObject("citas").put("type", "array").set("items", cita);
    hallazgo.putArray("required").add("tipo").add("texto").add("citas");

    ObjectNode raiz = objectMapper.createObjectNode();
    raiz.put("type", "object");
    raiz.put("additionalProperties", false);
    raiz.putObject("properties").putObject("hallazgos").put("type", "array").set("items", hallazgo);
    raiz.putArray("required").add("hallazgos");

    ObjectNode formato = objectMapper.createObjectNode();
    formato.put("type", "json_schema");
    ObjectNode esquema = formato.putObject("json_schema");
    esquema.put("name", "analisis_comentarios");
    esquema.put("strict", true);
    esquema.set("schema", raiz);
    return formato;
  }

  private static void exigir(String valor, String propiedad) {
    if (!StringUtils.hasText(valor)) {
      throw new IllegalStateException("Falta " + propiedad + " para usar el proveedor de IA");
    }
  }
}
