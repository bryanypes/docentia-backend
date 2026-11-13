package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.exception.CredencialesInvalidasException;
import java.util.Map;

/**
 * Cuerpo del 401 por credenciales: el error de siempre más cuántos intentos lleva el correo. El
 * mensaje sigue sin decir si falló el correo o la contraseña.
 */
public record CredencialesInvalidasResponse(
    String mensaje, Map<String, String> errores, Integer intentosUsados, Integer intentosMaximos) {

  public static CredencialesInvalidasResponse desde(CredencialesInvalidasException excepcion) {
    return new CredencialesInvalidasResponse(
        excepcion.getMessage(),
        Map.of(),
        excepcion.getIntentosUsados(),
        excepcion.getIntentosMaximos());
  }
}
