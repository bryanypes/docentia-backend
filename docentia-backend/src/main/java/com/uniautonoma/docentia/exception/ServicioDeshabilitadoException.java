package com.uniautonoma.docentia.exception;

/** La función existe pero está apagada en este ambiente por configuración. */
public class ServicioDeshabilitadoException extends RuntimeException {

  public ServicioDeshabilitadoException(String mensaje) {
    super(mensaje);
  }
}
