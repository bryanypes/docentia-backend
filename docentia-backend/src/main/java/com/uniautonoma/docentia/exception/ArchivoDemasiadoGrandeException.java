package com.uniautonoma.docentia.exception;

/** El archivo subido supera el tamaño permitido. */
public class ArchivoDemasiadoGrandeException extends RuntimeException {

  public ArchivoDemasiadoGrandeException(String mensaje) {
    super(mensaje);
  }
}
