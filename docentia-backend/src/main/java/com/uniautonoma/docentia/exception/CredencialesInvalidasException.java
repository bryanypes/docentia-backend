package com.uniautonoma.docentia.exception;

public class CredencialesInvalidasException extends RuntimeException {

  public CredencialesInvalidasException() {
    super("Correo o contraseña incorrectos");
  }
}
