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
import org.springframework.web.client.RestClientResponseException;

/**
 * Cliente para proveedores que exponen el formato chat/completions, que comparten muchos servicios
 * comerciales y servidores locales. El proveedor concreto, el modelo y la clave salen de la
 * configuración; para un proveedor con otro formato se agrega otra implementación de {@link
 * ModeloIA}.
 *
 * <p>Para gastar pocos tokens: la instrucción fija va primero (así la caché de prompt del
 * proveedor, si la tiene, la reutiliza), los comentarios viajan como {@code [id, texto]} en JSON
 * compacto y el modelo responde citando ids, no texto.
 */
public class ModeloIAChatCompletions implements ModeloIA {

  static final String RUTA = "/chat/completions";
  static final int SALIDA_POR_DEFECTO = 1500;

  private final RestClient restClient;
  private final String modelo;
  private final boolean enviarTemperatura;
  private final int maxTokensSalida;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public ModeloIAChatCompletions(RestClient.Builder restClientBuilder, IaProperties propiedades) {
    this(restClientBuilder, propiedades, SALIDA_POR_DEFECTO);
  }

  /**
   * @param restClientBuilder builder con los tiempos de espera ya configurados
   * @param maxTokensSalida tope de salida que se le pide al proveedor
   * @throws IllegalStateException si falta la dirección, el modelo o la clave
   */
  public ModeloIAChatCompletions(
      RestClient.Builder restClientBuilder, IaProperties propiedades, int maxTokensSalida) {
    exigir(propiedades.urlBase(), "docentia.ia.url-base");
    exigir(propiedades.modelo(), "docentia.ia.modelo");
    exigir(propiedades.clave(), "docentia.ia.clave (variable DOCENTIA_IA_CLAVE)");
    this.modelo = propiedades.modelo();
    this.enviarTemperatura = propiedades.enviarTemperatura();
    this.maxTokensSalida = maxTokensSalida;
    this.restClient =
        restClientBuilder
            .baseUrl(propiedades.urlBase())
            .defaultHeaders(cabeceras -> cabeceras.setBearerAuth(propiedades.clave()))
            .build();
  }

  @Override
  public RespuestaModelo analizar(PeticionAnalisis peticion) {
    String cuerpo = cuerpo(peticion);
    try {
      JsonNode respuesta =
          restClient
              .post()
              .uri(RUTA)
              .contentType(MediaType.APPLICATION_JSON)
              .body(cuerpo)
              .retrieve()
              .body(JsonNode.class);
      JsonNode contenido = respuesta == null ? null : respuesta.at("/choices/0/message/content");
      if (contenido == null || !contenido.isTextual()) {
        throw new ModeloIAException("El proveedor respondió sin contenido", null);
      }
      String modeloUsado = respuesta.path("model").asText(modelo);
      return new RespuestaModelo(
          contenido.asText(), modeloUsado, uso(respuesta.path("usage"), cuerpo, contenido));
    } catch (RestClientResponseException excepcion) {
      // Solo el código: el cuerpo del error podría repetir parte de la petición.
      throw new ModeloIAException(
          "El proveedor respondió HTTP " + excepcion.getStatusCode().value(), null);
    } catch (RestClientException excepcion) {
      throw new ModeloIAException(
          "Falló la llamada al proveedor: " + excepcion.getClass().getSimpleName(), null);
    }
  }

  /** Lee el uso que informa el proveedor; si no viene, lo estima por caracteres. */
  static Uso uso(JsonNode uso, String cuerpo, JsonNode contenido) {
    if (uso.path("prompt_tokens").isNumber()) {
      return new Uso(
          uso.path("prompt_tokens").asInt(),
          uso.path("completion_tokens").asInt(),
          uso.at("/prompt_tokens_details/cached_tokens").asInt(0),
          false);
    }
    return new Uso(
        EstimadorTokens.estimar(cuerpo), EstimadorTokens.estimar(contenido.asText()), 0, true);
  }

  String cuerpo(PeticionAnalisis peticion) {
    ObjectNode cuerpo = objectMapper.createObjectNode();
    cuerpo.put("model", modelo);
    if (enviarTemperatura) {
      cuerpo.put("temperature", 0);
    }
    cuerpo.put("max_tokens", maxTokensSalida);
    cuerpo.set("response_format", formatoJson());
    ArrayNode mensajes = cuerpo.putArray("messages");
    mensajes.addObject().put("role", "system").put("content", peticion.instruccion());
    mensajes.addObject().put("role", "user").put("content", entrada(peticion));
    try {
      return objectMapper.writeValueAsString(cuerpo);
    } catch (JsonProcessingException excepcion) {
      throw new ModeloIAException("No se pudo armar la petición", excepcion);
    }
  }

  /** {@code {"t":["D05+4"],"c":[[7,"texto"]]}}: los datos, sin nombres de campo repetidos. */
  String entrada(PeticionAnalisis peticion) {
    ObjectNode entrada = objectMapper.createObjectNode();
    ArrayNode temas = entrada.putArray("t");
    peticion.temas().forEach(temas::add);
    ArrayNode comentarios = entrada.putArray("c");
    peticion
        .comentarios()
        .forEach(comentario -> comentarios.addArray().add(comentario.id()).add(comentario.texto()));
    try {
      return objectMapper.writeValueAsString(entrada);
    } catch (JsonProcessingException excepcion) {
      throw new ModeloIAException("No se pudo armar la petición", excepcion);
    }
  }

  /**
   * Esquema compacto. {@code json_schema} con {@code strict} funciona en todos los proveedores que
   * hablan este formato; algunos rechazan {@code json_object}.
   */
  private ObjectNode formatoJson() {
    ObjectNode hallazgo = objectMapper.createObjectNode();
    hallazgo.put("type", "object");
    hallazgo.put("additionalProperties", false);
    ObjectNode propiedades = hallazgo.putObject("properties");
    propiedades.putObject("t").put("type", "string").putArray("enum").add("f").add("o");
    propiedades.putObject("x").put("type", "string");
    propiedades.putObject("c").put("type", "array").putObject("items").put("type", "integer");
    hallazgo.putArray("required").add("t").add("x").add("c");

    ObjectNode raiz = objectMapper.createObjectNode();
    raiz.put("type", "object");
    raiz.put("additionalProperties", false);
    ObjectNode campos = raiz.putObject("properties");
    campos.putObject("h").put("type", "array").set("items", hallazgo);
    campos.putObject("r").put("type", "string");
    raiz.putArray("required").add("h").add("r");

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
