package com.uniautonoma.docentia.security;

import com.uniautonoma.docentia.exception.CredencialesInvalidasException;

/** Valida un ID token de Google y devuelve el correo verificado. */
public interface VerificadorTokenGoogle {

  /**
   * @param idToken JWT emitido por Google Identity Services
   * @return correo del usuario en minúsculas
   * @throws CredencialesInvalidasException si el token no es válido o no trae correo
   */
  String validarYExtraerCorreo(String idToken);
}
