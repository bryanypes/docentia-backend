package com.uniautonoma.docentia.entity;

import java.util.Locale;
import java.util.Map;
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
  /**
   * Palabras académicas que los sistemas de origen guardan sin tilde ("ADMINISTRACION"). Solo se
   * corrigen al normalizar un nombre en mayúsculas o minúsculas; la migración V32 aplica la misma
   * lista a lo ya guardado.
   */
  private static final Map<String, String> TILDES =
      Map.ofEntries(
          Map.entry("administracion", "administración"),
          Map.entry("ingenieria", "ingeniería"),
          Map.entry("educacion", "educación"),
          Map.entry("comunicacion", "comunicación"),
          Map.entry("informacion", "información"),
          Map.entry("investigacion", "investigación"),
          Map.entry("gestion", "gestión"),
          Map.entry("electronica", "electrónica"),
          Map.entry("mecanica", "mecánica"),
          Map.entry("informatica", "informática"),
          Map.entry("telematica", "telemática"),
          Map.entry("tecnologia", "tecnología"),
          Map.entry("tecnologica", "tecnológica"),
          Map.entry("tecnologico", "tecnológico"),
          Map.entry("tecnica", "técnica"),
          Map.entry("tecnico", "técnico"),
          Map.entry("psicologia", "psicología"),
          Map.entry("pedagogia", "pedagogía"),
          Map.entry("biologia", "biología"),
          Map.entry("metodologia", "metodología"),
          Map.entry("contaduria", "contaduría"),
          Map.entry("economia", "economía"),
          Map.entry("economicas", "económicas"),
          Map.entry("juridicas", "jurídicas"),
          Map.entry("politicas", "políticas"),
          Map.entry("publica", "pública"),
          Map.entry("publico", "público"),
          Map.entry("basica", "básica"),
          Map.entry("basicas", "básicas"),
          Map.entry("matematica", "matemática"),
          Map.entry("matematicas", "matemáticas"),
          Map.entry("fisica", "física"),
          Map.entry("quimica", "química"),
          Map.entry("estadistica", "estadística"),
          Map.entry("logistica", "logística"),
          Map.entry("energetica", "energética"),
          Map.entry("calculo", "cálculo"),
          Map.entry("algebra", "álgebra"),
          Map.entry("etica", "ética"),
          Map.entry("analisis", "análisis"),
          Map.entry("diseno", "diseño"),
          Map.entry("produccion", "producción"),
          Map.entry("construccion", "construcción"),
          Map.entry("innovacion", "innovación"),
          Map.entry("formacion", "formación"),
          Map.entry("evaluacion", "evaluación"),
          Map.entry("organizacion", "organización"),
          Map.entry("direccion", "dirección"),
          Map.entry("programacion", "programación"),
          Map.entry("academica", "académica"),
          Map.entry("academico", "académico"));
  private static final Pattern ROMANO = Pattern.compile("\\(?(I|II|III|IV|V|VI|VII|VIII|IX|X)\\)?");
  private static final Pattern ESPACIOS = Pattern.compile("\\s+");

  private NombrePropio() {}

  /**
   * @param texto nombre tal como llegó
   * @return el mismo texto sin espacios de más y, si venía todo en mayúsculas o todo en minúsculas,
   *     con mayúscula inicial en cada palabra salvo los conectores y las tildes que faltaban
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
    String original = palabra.toLowerCase(ESPANOL);
    String minuscula = TILDES.getOrDefault(original, original);
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
