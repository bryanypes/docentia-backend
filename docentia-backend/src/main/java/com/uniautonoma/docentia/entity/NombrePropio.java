package com.uniautonoma.docentia.entity;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Nombres de personas, programas, facultades y cursos con mayúscula inicial. Campus y los CSV
 * suelen traerlos todo en mayúsculas o todo en minúsculas; solo esos se normalizan, para no tocar
 * uno ya bien escrito ("ELECTIVA I (Optativa)", "Ingeniería de Software"). La migración V31 aplica
 * la misma regla a lo ya guardado.
 */
public final class NombrePropio {

  private static final Locale ESPANOL = Locale.forLanguageTag("es-CO");
  private static final Set<String> CONECTORES =
      Set.of(
          "a", "al", "con", "de", "del", "e", "el", "en", "la", "las", "los", "o", "para", "por",
          "u", "y");
  private static final Pattern ROMANO = Pattern.compile("\\(?(I|II|III|IV|V|VI|VII|VIII|IX|X)\\)?");
  private static final Pattern ESPACIOS = Pattern.compile("\\s+");

  private NombrePropio() {}

  /**
   * @param texto nombre tal como llegó
   * @return el mismo texto sin espacios de más y, si venía todo en mayúsculas o todo en minúsculas,
   *     con mayúscula inicial en cada palabra salvo los conectores
   */
  public static String normalizar(String texto) {
    if (texto == null) {
      return null;
    }
    String limpio = ESPACIOS.matcher(texto.strip()).replaceAll(" ");
    boolean tieneLetras = limpio.codePoints().anyMatch(Character::isLetter);
    boolean uniforme =
        limpio.equals(limpio.toUpperCase(ESPANOL)) || limpio.equals(limpio.toLowerCase(ESPANOL));
    if (!tieneLetras || !uniforme) {
      return limpio;
    }
    String[] palabras = limpio.split(" ");
    StringBuilder resultado = new StringBuilder();
    for (int i = 0; i < palabras.length; i++) {
      if (i > 0) {
        resultado.append(' ');
      }
      resultado.append(palabra(palabras[i], i == 0));
    }
    return resultado.toString();
  }

  private static String palabra(String palabra, boolean primera) {
    String minuscula = palabra.toLowerCase(ESPANOL);
    if (!primera && CONECTORES.contains(minuscula)) {
      return minuscula;
    }
    if (ROMANO.matcher(palabra.toUpperCase(ESPANOL)).matches()) {
      return palabra.toUpperCase(ESPANOL);
    }
    // Mayúscula tras cada separador que no es letra: "(optativa)" → "(Optativa)", "jean-paul" →
    // "Jean-Paul".
    StringBuilder salida = new StringBuilder(minuscula.length());
    boolean inicio = true;
    for (int i = 0; i < minuscula.length(); ) {
      int codigo = minuscula.codePointAt(i);
      if (Character.isLetter(codigo)) {
        salida.appendCodePoint(inicio ? Character.toTitleCase(codigo) : codigo);
        inicio = false;
      } else {
        salida.appendCodePoint(codigo);
        inicio = codigo != '\'';
      }
      i += Character.charCount(codigo);
    }
    return salida.toString();
  }
}
