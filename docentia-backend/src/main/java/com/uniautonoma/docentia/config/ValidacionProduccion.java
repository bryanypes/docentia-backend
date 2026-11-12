package com.uniautonoma.docentia.config;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Revisiones al arrancar en producción. Un secreto de desarrollo detiene la app. Sin Google solo
 * avisa: el personal entra con contraseña, pero los estudiantes no tienen ninguna utilizable.
 */
@Configuration
@Profile("prod")
public class ValidacionProduccion {

  private static final Logger LOG = LoggerFactory.getLogger(ValidacionProduccion.class);

  public ValidacionProduccion(
      @Value("${docentia.jwt.secreto:}") String secreto, GoogleProperties google) {
    exigirSecretoPropio(secreto);
    avisoGoogle(google).ifPresent(LOG::warn);
  }

  static void exigirSecretoPropio(String secreto) {
    String normalizado = secreto == null ? "" : secreto.toLowerCase(java.util.Locale.ROOT);
    if (normalizado.contains("no-usar") || normalizado.contains("desarrollo")) {
      throw new IllegalStateException(
          "DOCENTIA_JWT_SECRETO tiene un valor de desarrollo; define uno propio");
    }
  }

  /** Mensaje para el log si falta el inicio de sesión con Google; vacío si está configurado. */
  static Optional<String> avisoGoogle(GoogleProperties google) {
    if (google != null && google.habilitado()) {
      return Optional.empty();
    }
    return Optional.of(
        "Falta DOCENTIA_GOOGLE_CLIENT_ID: los estudiantes no podrán iniciar sesión (fuera de dev"
            + " no tienen contraseña) y POST /sesiones/google responderá 503. Configura el mismo"
            + " Client ID en DOCENTIA_GOOGLE_CLIENT_ID (API) y en VITE_GOOGLE_CLIENT_ID (build"
            + " de la web). Ver docs/OPERACION.md, sección Inicio de sesión con Google.");
  }
}
