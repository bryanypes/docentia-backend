package com.uniautonoma.docentia.exception;

public class CredencialesInvalidasException extends RuntimeException {

  /** Intentos fallidos del correo en la ventana actual; nulo fuera del login con contraseña. */
  private final Integer intentosUsados;

  private final Integer intentosMaximos;

  public CredencialesInvalidasException() {
    this(null, null);
  }

  private CredencialesInvalidasException(Integer intentosUsados, Integer intentosMaximos) {
    super("Correo o contraseña incorrectos");
    this.intentosUsados = intentosUsados;
    this.intentosMaximos = intentosMaximos;
  }

  /** El mismo rechazo con el conteo de intentos, para que la persona sepa cuántos le quedan. */
  public CredencialesInvalidasException conIntentos(int usados, int maximos) {
    return new CredencialesInvalidasException(usados, maximos);
  }

  public Integer getIntentosUsados() {
    return intentosUsados;
  }

  public Integer getIntentosMaximos() {
    return intentosMaximos;
  }
}
