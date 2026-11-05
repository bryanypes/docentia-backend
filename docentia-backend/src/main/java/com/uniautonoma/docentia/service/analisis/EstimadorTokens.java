package com.uniautonoma.docentia.service.analisis;

/**
 * Estimación de tokens por caracteres, para topes y para cuando el proveedor no informa el uso. En
 * español con tokenizadores comunes rinde entre 3 y 4 caracteres por token; se usa 3,5 para no
 * quedarse corto. Es una aproximación: el número real lo da el proveedor.
 */
public final class EstimadorTokens {

  static final double CARACTERES_POR_TOKEN = 3.5;

  private EstimadorTokens() {}

  public static int estimar(String texto) {
    return texto == null ? 0 : (int) Math.ceil(texto.length() / CARACTERES_POR_TOKEN);
  }

  public static int estimarCaracteres(long caracteres) {
    return (int) Math.ceil(caracteres / CARACTERES_POR_TOKEN);
  }
}
