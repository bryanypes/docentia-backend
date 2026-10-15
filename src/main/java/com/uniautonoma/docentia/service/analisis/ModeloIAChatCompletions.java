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
    cuerpo.putObject("response_format").put("type", "json_object");
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

  private static void exigir(String valor, String propiedad) {
    if (!StringUtils.hasText(valor)) {
      throw new IllegalStateException("Falta " + propiedad + " para usar el proveedor de IA");
    }
  }
}
