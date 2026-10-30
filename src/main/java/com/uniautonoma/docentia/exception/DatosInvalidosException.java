package com.uniautonoma.docentia.exception;

/**
 * Los datos recibidos no cumplen una validación que depende de varios campos a la vez.
 *
 * <p>Si el problema es de un campo concreto se indica en {@code campo}, y el 400 lo devuelve en
 * {@code errores} como la validación de formularios.
 */
public class DatosInvalidosException extends RuntimeException {

  private final String campo;

  public DatosInvalidosException(String mensaje) {
    this(mensaje, null);
  }

  public DatosInvalidosException(String mensaje, String campo) {
    super(mensaje);
    this.campo = campo;
  }

  public String getCampo() {
    return campo;
  }
}
