package com.uniautonoma.docentia.exception;

/** Los datos recibidos no cumplen una validación que depende de varios campos a la vez. */
public class DatosInvalidosException extends RuntimeException {

  public DatosInvalidosException(String mensaje) {
    super(mensaje);
  }
}
