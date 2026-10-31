package com.uniautonoma.docentia.exception;

/** La petición está bien formada, pero lo que pide no se puede hacer con estos datos (422). */
public class NoProcesableException extends RuntimeException {

  public NoProcesableException(String mensaje) {
    super(mensaje);
  }
}
