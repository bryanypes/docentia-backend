package com.uniautonoma.docentia.service.analisis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.core.io.Resource;

/**
 * Filtro con reglas leídas de un archivo editable (por defecto
 * resources/filtros/reglas-comentarios.txt). Cada línea es "CATEGORIA | expresión regular" y se
 * compara contra el texto normalizado: minúsculas, sin tildes y con espacios simples.
 */
public class FiltroPorReglas implements FiltroComentarios {

  private record Regla(String categoria, String texto, Pattern patron) {}

  private final List<Regla> reglas;

  /**
   * @throws IllegalStateException si una línea no tiene el formato o la expresión es inválida
   */
  public FiltroPorReglas(Resource archivo) {
    try (InputStream entrada = archivo.getInputStream()) {
      this.reglas =
          leer(new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8)));
    } catch (IOException excepcion) {
      throw new UncheckedIOException("No se pudo leer el archivo de reglas " + archivo, excepcion);
    }
  }

  @Override
  public Optional<Exclusion> evaluar(String comentario) {
    String normalizado = normalizar(comentario);
    return reglas.stream()
        .filter(regla -> regla.patron().matcher(normalizado).find())
        .findFirst()
        .map(regla -> new Exclusion(regla.categoria(), regla.texto()));
  }

  public int cantidadDeReglas() {
    return reglas.size();
  }

  /** Minúsculas, sin tildes y con un solo espacio entre palabras. */
  static String normalizar(String texto) {
    return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("\\s+", " ")
        .strip();
  }

  private static List<Regla> leer(BufferedReader lector) throws IOException {
    List<Regla> reglas = new ArrayList<>();
    String linea;
    int numero = 0;
    while ((linea = lector.readLine()) != null) {
      numero++;
      String limpia = linea.strip();
      if (limpia.isEmpty() || limpia.startsWith("#")) {
        continue;
      }
      int separador = limpia.indexOf('|');
      if (separador <= 0) {
        throw new IllegalStateException(
            "Regla del filtro sin el formato 'CATEGORIA | expresión' en la línea " + numero);
      }
      String categoria = limpia.substring(0, separador).strip().toUpperCase(Locale.ROOT);
      String expresion = limpia.substring(separador + 1).strip();
      try {
        reglas.add(new Regla(categoria, expresion, Pattern.compile(expresion)));
      } catch (PatternSyntaxException invalida) {
        throw new IllegalStateException(
            "Expresión inválida en la línea "
                + numero
                + " del filtro: "
                + invalida.getDescription());
      }
    }
    return List.copyOf(reglas);
  }
}
