package com.uniautonoma.docentia.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la aplicación. Las ventanas de cada etapa se comparan contra la fecha de Colombia, no
 * contra la del servidor; las pruebas lo reemplazan por un reloj fijo.
 */
@Configuration
public class RelojConfig {

  public static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

  @Bean
  Clock reloj() {
    return Clock.system(ZONA_COLOMBIA);
  }
}
