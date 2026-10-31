package com.uniautonoma.docentia.service.analisis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniautonoma.docentia.entity.TipoHallazgo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Valida que la respuesta del modelo sea el JSON pedido y la convierte en hallazgos propuestos. Si
 * no cumple el formato, lanza {@link RespuestaInvalidaException} y el procesador reintenta una vez.
 */
public class LectorRespuestaAnalisis {

  static final int MAXIMO_HALLAZGOS = 10;
  static final int LONGITUD_MAXIMA = 1000;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public List<HallazgoPropuesto> leer(String contenido) {
    JsonNode raiz = parsear(quitarCercoMarkdown(contenido));
    JsonNode hallazgos = raiz.path("hallazgos");
    if (!hallazgos.isArray()) {
      throw new RespuestaInvalidaException("Falta la lista \"hallazgos\"");
    }
    if (hallazgos.size() > MAXIMO_HALLAZGOS) {
      throw new RespuestaInvalidaException("Hay más de " + MAXIMO_HALLAZGOS + " hallazgos");
    }
    List<HallazgoPropuesto> propuestos = new ArrayList<>();
    for (JsonNode hallazgo : hallazgos) {
      propuestos.add(leerHallazgo(hallazgo));
    }
    return propuestos;
  }

  private HallazgoPropuesto leerHallazgo(JsonNode nodo) {
    TipoHallazgo tipo =
        TipoHallazgo.desdeCodigo(nodo.path("tipo").asText())
            .orElseThrow(() -> new RespuestaInvalidaException("Tipo de hallazgo desconocido"));
    String texto = textoObligatorio(nodo.path("texto"), "texto");
    JsonNode citas = nodo.path("citas");
    if (!citas.isArray()) {
      throw new RespuestaInvalidaException("Cada hallazgo debe traer la lista \"citas\"");
    }
    List<CitaPropuesta> propuestas = new ArrayList<>();
    for (JsonNode cita : citas) {
      propuestas.add(
          new CitaPropuesta(
              leerIdentificador(cita.path("comentarioId")).orElse(null),
              textoObligatorio(cita.path("fragmento"), "fragmento")));
    }
    return new HallazgoPropuesto(tipo, texto, propuestas);
  }

  private Optional<Long> leerIdentificador(JsonNode nodo) {
    if (nodo.canConvertToLong() && nodo.isIntegralNumber()) {
      return Optional.of(nodo.asLong());
    }
    if (nodo.isTextual() && nodo.asText().matches("\\d{1,18}")) {
      return Optional.of(Long.parseLong(nodo.asText()));
    }
    return Optional.empty();
  }

  private String textoObligatorio(JsonNode nodo, String campo) {
    if (!nodo.isTextual() || nodo.asText().isBlank()) {
      throw new RespuestaInvalidaException("Falta el campo \"" + campo + "\"");
    }
    String texto = nodo.asText().strip();
    if (texto.length() > LONGITUD_MAXIMA) {
      throw new RespuestaInvalidaException("El campo \"" + campo + "\" es demasiado largo");
    }
    return texto;
  }

  private JsonNode parsear(String contenido) {
    try {
      JsonNode raiz = objectMapper.readTree(contenido);
      if (raiz == null || !raiz.isObject()) {
        throw new RespuestaInvalidaException("La respuesta no es un objeto JSON");
      }
      return raiz;
    } catch (JsonProcessingException excepcion) {
      throw new RespuestaInvalidaException("La respuesta no es JSON válido");
    }
  }

  /** Algunos modelos envuelven el JSON en un bloque ```json … ```. */
  private String quitarCercoMarkdown(String contenido) {
    String texto = contenido == null ? "" : contenido.strip();
    if (texto.startsWith("```")) {
      texto = texto.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
    }
    return texto;
  }

  public record HallazgoPropuesto(TipoHallazgo tipo, String texto, List<CitaPropuesta> citas) {}

  /**
   * @param comentarioId nulo si el modelo no dio un identificador numérico
   */
  public record CitaPropuesta(Long comentarioId, String fragmento) {}

  /** La respuesta del modelo no tiene el formato pedido. */
  public static class RespuestaInvalidaException extends RuntimeException {

    public RespuestaInvalidaException(String motivo) {
      super(motivo);
    }
  }
}
