package com.uniautonoma.docentia.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Configuration;

/**
 * Parámetros de los indicadores generales.
 *
 * @param destacadoUmbral promedio ponderado mínimo, inclusive, para contar a un docente como
 *     destacado
 */
@ConfigurationProperties(prefix = "docentia.kpi")
public record KpiProperties(@DefaultValue("4.5") BigDecimal destacadoUmbral) {

  @Configuration
  @EnableConfigurationProperties(KpiProperties.class)
  static class Registro {}
}
