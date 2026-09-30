package com.uniautonoma.docentia;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import javax.sql.DataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Levanta un PostgreSQL embebido para que las migraciones Flyway corran contra el motor real. */
@TestConfiguration
public class BaseDatosPruebaConfig {

  @Bean(destroyMethod = "close")
  EmbeddedPostgres postgresEmbebido() throws IOException {
    return EmbeddedPostgres.start();
  }

  @Bean
  DataSource dataSource(EmbeddedPostgres postgresEmbebido) {
    return postgresEmbebido.getPostgresDatabase();
  }
}
