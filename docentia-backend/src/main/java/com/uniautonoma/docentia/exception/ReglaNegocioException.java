package com.uniautonoma.docentia.exception;

/** La petición es válida en su forma, pero choca con el estado actual de los datos. */
public class ReglaNegocioException extends RuntimeException {

  public ReglaNegocioException(String mensaje) {
    super(mensaje);
  }
}
