package com.uniautonoma.docentia.service.analisis;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Quita del comentario los datos que podrían identificar al estudiante antes de enviarlo al modelo:
 * correos, enlaces, usuarios de redes, códigos estudiantiles, números largos (cédula, teléfono) y
 * el nombre cuando el estudiante se presenta ("me llamo…", "soy…"). Es un filtro por patrones: no
 * reconoce un nombre propio suelto en medio del texto.
 */
public class Anonimizador {

  private record Regla(Pattern patron, String reemplazo) {}

  private static final List<Regla> REGLAS =
      List.of(
          new Regla(Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+"), "[correo]"),
          new Regla(Pattern.compile("(?i)\\b(https?://|www\\.)\\S+"), "[enlace]"),
          new Regla(Pattern.compile("(?<![\\w])@\\w{2,}"), "[usuario]"),
          new Regla(Pattern.compile("\\b[A-Za-z]\\d{5,}\\b"), "[código]"),
          new Regla(Pattern.compile("\\b\\d[\\d .-]{4,}\\d\\b"), "[número]"),
          new Regla(
              Pattern.compile(
                  "(?iu)\\b(me llamo|mi nombre es|soy el estudiante|soy la estudiante|atentamente,?)"
                      + "\\s+\\p{Lu}\\p{L}+(\\s+\\p{Lu}\\p{L}+)?"),
              "$1 [nombre]"),
          new Regla(
              Pattern.compile(
                  "(?u)\\b(compañer[oa]|amig[oa]|profe|profesora?|docente|el estudiante|la estudiante)"
                      + "\\s+\\p{Lu}\\p{L}+(\\s+\\p{Lu}\\p{L}+)?"),
              "$1 [nombre]"),
          // Dos palabras seguidas con mayúscula que no abren la frase: casi siempre un nombre.
          new Regla(
              Pattern.compile("(?u)(?<=[\\p{Ll},;:]\\s)\\p{Lu}\\p{Ll}+\\s+\\p{Lu}\\p{Ll}+"),
              "[nombre]"));

  /** Devuelve el texto sin datos identificables; siempre igual para el mismo texto. */
  /** Además de las reglas, reemplaza el nombre del docente evaluado si aparece. */
  public String anonimizar(String texto, String nombreDocente) {
    String resultado = anonimizar(texto);
    if (nombreDocente == null) {
      return resultado;
    }
    for (String parte : nombreDocente.split("\\s+")) {
      if (parte.length() >= 3) {
        resultado =
            Pattern.compile("(?iu)\\b" + Pattern.quote(parte) + "\\b")
                .matcher(resultado)
                .replaceAll("[docente]");
      }
    }
    return resultado;
  }

  public String anonimizar(String texto) {
    String resultado = texto;
    for (Regla regla : REGLAS) {
      resultado = regla.patron().matcher(resultado).replaceAll(regla.reemplazo());
    }
    return resultado;
  }
}
