package com.uniautonoma.docentia.service.analisis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Modelo de mentira para desarrollo y pruebas: no usa red y para los mismos comentarios responde
 * siempre lo mismo. Clasifica cada comentario como fortaleza u oportunidad de mejora con una lista
 * corta de palabras y cita la primera frase del comentario, copiada tal cual.
 */
public class ModeloIASimulado implements ModeloIA {

  public static final String NOMBRE = "simulado-determinista";
  static final int MAXIMO_POR_TIPO = 3;

  private static final Pattern NEGATIVO =
      Pattern.compile(
          "\\b(no|nunca|tarde|demora\\w*|desorganiz\\w*|confus\\w*|rapido|falta\\w*|deberia\\w*|"
              + "mejorar|dificil\\w*|injust\\w*|pesim\\w*|aburrid\\w*|poco)\\b");
  private static final Pattern PRIMERA_FRASE = Pattern.compile("^[^.;!?\\n]{3,}");

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public RespuestaModelo analizar(PeticionAnalisis peticion) {
    ObjectNode raiz = objectMapper.createObjectNode();
    ArrayNode hallazgos = raiz.putArray("hallazgos");
    int fortalezas = 0;
    int oportunidades = 0;
    for (ComentarioAnonimo comentario : peticion.comentarios()) {
      boolean esNegativo = NEGATIVO.matcher(normalizar(comentario.texto())).find();
      if (esNegativo && oportunidades < MAXIMO_POR_TIPO) {
        agregar(hallazgos, "oportunidad_de_mejora", "Los estudiantes señalan: ", comentario);
        oportunidades++;
      } else if (!esNegativo && fortalezas < MAXIMO_POR_TIPO) {
        agregar(hallazgos, "fortaleza", "Los estudiantes valoran: ", comentario);
        fortalezas++;
      }
    }
    try {
      return new RespuestaModelo(objectMapper.writeValueAsString(raiz), NOMBRE);
    } catch (JsonProcessingException excepcion) {
      throw new ModeloIAException("No se pudo armar la respuesta simulada", excepcion);
    }
  }

  private void agregar(
      ArrayNode hallazgos, String tipo, String prefijo, ComentarioAnonimo comentario) {
    String fragmento = primeraFrase(comentario.texto());
    ObjectNode hallazgo = hallazgos.addObject();
    hallazgo.put("tipo", tipo);
    hallazgo.put("texto", prefijo + fragmento.toLowerCase(Locale.ROOT) + ".");
    ObjectNode cita = hallazgo.putArray("citas").addObject();
    cita.put("comentarioId", comentario.id());
    cita.put("fragmento", fragmento);
  }

  private String primeraFrase(String texto) {
    String limpio = texto.strip();
    Matcher frase = PRIMERA_FRASE.matcher(limpio);
    String resultado = frase.find() ? frase.group().strip() : limpio;
    List<String> palabras = List.of(resultado.split("\\s+"));
    return palabras.size() <= 15 ? resultado : String.join(" ", palabras.subList(0, 15));
  }

  private String normalizar(String texto) {
    return Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }
}
