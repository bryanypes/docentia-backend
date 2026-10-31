package com.uniautonoma.docentia.exception;

/** Límite de intentos de inicio de sesión. El mensaje no dice si el usuario existe. */
public class DemasiadosIntentosException extends RuntimeException {

  public DemasiadosIntentosException() {
    super("Demasiados intentos. Espera unos minutos y vuelve a intentarlo");
  }
}
