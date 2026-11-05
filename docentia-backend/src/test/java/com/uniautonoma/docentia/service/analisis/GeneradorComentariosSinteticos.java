package com.uniautonoma.docentia.service.analisis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;

/**
 * Comentarios sintéticos para pruebas de carga, armados a partir del conjunto de validación: se
 * combinan frases con saludos, firmas y relleno para tener variedad de largo y de forma. Es
 * determinista: el mismo N da los mismos comentarios. Ninguno es de un estudiante real.
 */
public final class GeneradorComentariosSinteticos {

  private static final String[] INICIOS = {
    "", "Hola profe, ", "Buenas tardes. ", "La verdad, ", "En general ", "Profe: ", ""
  };
  private static final String[] FINALES = {
    "", " Gracias.", " Saludos.", "", " En el grupo de la tarde pasa igual.", ""
  };
  private static final String[] RELLENO = {
    "Además, las clases empiezan a tiempo y el ambiente del salón es tranquilo para aprender.",
    "Me gustaría que hubiera más talleres prácticos antes de los parciales para practicar.",
    "En las tutorías resolvió dudas de proyectos y explicó otra vez los temas difíciles.",
    "A veces el material llega tarde a la plataforma y cuesta organizarse con las entregas."
  };

  private GeneradorComentariosSinteticos() {}

  /** Textos útiles del conjunto de validación (sin los que deben excluirse). */
  public static List<String> base() {
    try (InputStream entrada =
        new ClassPathResource("ia/validacion-comentarios.json").getInputStream()) {
      JsonNode datos = new ObjectMapper().readTree(entrada);
      List<String> textos = new ArrayList<>();
      for (JsonNode c : datos.path("comentarios")) {
        if (c.path("excluir").isNull() && c.path("sensible").isNull()) {
          textos.add(c.path("texto").asText());
        }
      }
      return textos;
    } catch (IOException fallo) {
      throw new UncheckedIOException(fallo);
    }
  }

  public static List<String> generar(int n) {
    List<String> base = base();
    List<String> comentarios = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      StringBuilder texto =
          new StringBuilder(INICIOS[i % INICIOS.length]).append(base.get(i % base.size()));
      if (i % 3 == 0) {
        texto.append(' ').append(base.get((i * 7 + 3) % base.size()));
      }
      if (i % 5 == 0) {
        texto.append(' ').append(RELLENO[i % RELLENO.length]);
      }
      if (i % 11 == 0) {
        texto
            .append(' ')
            .append(RELLENO[(i + 1) % RELLENO.length])
            .append(' ')
            .append(RELLENO[(i + 2) % RELLENO.length]);
      }
      texto.append(" (grupo ").append(i % 37).append(", semana ").append(i % 16 + 1).append(')');
      texto.append(FINALES[i % FINALES.length]);
      comentarios.add(texto.toString());
    }
    return comentarios;
  }
}
