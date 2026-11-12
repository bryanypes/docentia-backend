package com.uniautonoma.docentia.service.analisis.local;

import java.text.Normalizer;
import java.util.Locale;

/** Normaliza para guardar (NFC) y para comparar (minúsculas, sin tildes, sin letras repetidas). */
public final class NormalizadorTexto {

  private NormalizadorTexto() {}

  /** Recorta, compone en NFC y deja un solo espacio entre palabras. Es lo que se guarda. */
  public static String limpiar(String texto) {
    if (texto == null) {
      return "";
    }
    return Normalizer.normalize(texto, Normalizer.Form.NFC).strip().replaceAll("\\s+", " ");
  }

  /** Forma de comparación: no se muestra nunca. */
  public static String comparable(String texto) {
    String sinTildes =
        Normalizer.normalize(limpiar(texto), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    return sinTildes
        .toLowerCase(Locale.ROOT)
        .replaceAll("(\\p{L})\\1{2,}", "$1")
        .replaceAll("[^\\p{L}\\p{N}]+", " ")
        .strip();
  }

  /** Cuenta palabras con al menos dos letras. */
  public static int palabrasReales(String texto) {
    int cuenta = 0;
    for (String palabra : comparable(texto).split(" ")) {
      if (palabra.codePoints().filter(Character::isLetter).count() >= 2) {
        cuenta++;
      }
    }
    return cuenta;
  }
}
