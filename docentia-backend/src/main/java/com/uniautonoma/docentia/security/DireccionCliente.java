package com.uniautonoma.docentia.security;

import jakarta.servlet.http.HttpServletRequest;

/** IP del cliente. Vale el último salto de X-Forwarded-For, el que agregó nuestro proxy. */
public final class DireccionCliente {

  private DireccionCliente() {}

  public static String de(HttpServletRequest peticion) {
    String reenviada = peticion.getHeader("X-Forwarded-For");
    if (reenviada == null || reenviada.isBlank()) {
      return peticion.getRemoteAddr();
    }
    String[] saltos = reenviada.split(",");
    return saltos[saltos.length - 1].trim();
  }
}
