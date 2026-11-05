package com.uniautonoma.docentia.service.analisis;

/**
 * Estimación de tokens por caracteres, para topes y para cuando el proveedor no informa el uso. Se
 * calibró con el proveedor probado: los comentarios en español y el JSON dieron unos 2,3 caracteres
 * por token (ver docs/ANALISIS_IA_COSTOS.md). Es una aproximación: el número real lo da el
 * proveedor.
 */
public final class EstimadorTokens {

  static final double CARACTERES_POR_TOKEN = 2.3;

  private EstimadorTokens() {}

  public static int estimar(String texto) {
    return texto == null ? 0 : (int) Math.ceil(texto.length() / CARACTERES_POR_TOKEN);
  }

  public static int estimarCaracteres(long caracteres) {
    return (int) Math.ceil(caracteres / CARACTERES_POR_TOKEN);
  }
}
