package com.uniautonoma.docentia;

import com.uniautonoma.docentia.config.RelojConfig;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.sql.DataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Levanta un PostgreSQL embebido para que las migraciones Flyway corran contra el motor real, y
 * fija el reloj para que las ventanas de las etapas no dependan del día en que corren las pruebas.
 */
@TestConfiguration
public class BaseDatosPruebaConfig {

  /** Día en que "ocurren" todas las pruebas. */
  public static final LocalDate HOY = LocalDate.of(2026, 10, 20);

  @Bean(destroyMethod = "close")
  EmbeddedPostgres postgresEmbebido() throws IOException {
    return EmbeddedPostgres.start();
  }

  @Bean
  DataSource dataSource(EmbeddedPostgres postgresEmbebido) {
    return postgresEmbebido.getPostgresDatabase();
  }

  @Bean
  @Primary
  Clock relojFijo() {
    return Clock.fixed(
        HOY.atTime(LocalTime.NOON).atZone(RelojConfig.ZONA_COLOMBIA).toInstant(),
        RelojConfig.ZONA_COLOMBIA);
  }
}
