package com.uniautonoma.docentia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Configuration;

/**
 * Reglas del proceso de retroalimentación.
 *
 * @param exigirAsistencia el docente ve su resultado y firma el acta solo después de que la
 *     coordinación registre su asistencia a la cita
 */
@ConfigurationProperties(prefix = "docentia.proceso")
public record ProcesoProperties(@DefaultValue("true") boolean exigirAsistencia) {

  @Configuration
  @EnableConfigurationProperties(ProcesoProperties.class)
  static class Registro {}
}
