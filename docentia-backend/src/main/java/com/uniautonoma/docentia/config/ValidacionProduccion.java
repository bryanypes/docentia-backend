package com.uniautonoma.docentia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** En producción la app no arranca con un secreto de desarrollo. */
@Configuration
@Profile("prod")
public class ValidacionProduccion {

  public ValidacionProduccion(@Value("${docentia.jwt.secreto:}") String secreto) {
    exigirSecretoPropio(secreto);
  }

  static void exigirSecretoPropio(String secreto) {
    String normalizado = secreto == null ? "" : secreto.toLowerCase(java.util.Locale.ROOT);
    if (normalizado.contains("no-usar") || normalizado.contains("desarrollo")) {
      throw new IllegalStateException(
          "DOCENTIA_JWT_SECRETO tiene un valor de desarrollo; define uno propio");
    }
  }
}
