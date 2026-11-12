package com.uniautonoma.docentia.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Los callbacks de Flyway apagan el disparador de actas inmutables solo mientras se migra (V36 da
 * tipo a las actas existentes). Estas pruebas corren las migraciones reales sobre un PostgreSQL
 * embebido propio, con un acta firmada y una anulada creadas en V35.
 */
class MigracionesActasFirmadasTest {

  private static final String ESQUEMA = "ensayo_callbacks";
  private static EmbeddedPostgres postgres;
  private static DataSource fuente;
  private JdbcTemplate jdbc;

  @BeforeAll
  static void levantar() throws IOException {
    postgres = EmbeddedPostgres.start();
    fuente = postgres.getPostgresDatabase();
  }

  @AfterAll
  static void apagar() throws IOException {
    postgres.close();
  }

  @BeforeEach
  void limpiar() {
    jdbc = new JdbcTemplate(fuente);
    jdbc.execute("drop schema if exists " + ESQUEMA + " cascade");
    jdbc.execute("create schema " + ESQUEMA);
    jdbc.execute("set search_path to " + ESQUEMA);
  }

  @Test
  void conActasFirmadaYAnuladaLaMigracionPasaYLaFirmadaSigueInmutable() {
    flyway("35").migrate();
    sembrarActas();

    flyway(null).migrate();

    assertThat(disparadorEncendido()).isTrue();
    assertThat(tipos()).containsExactly("FIRMADA=RETROALIMENTACION", "ANULADA=COMPROMISO");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "update " + ESQUEMA + ".actas set promedio = 1 where estado = 'FIRMADA'"))
        .hasMessageContaining("Un acta firmada es inmutable");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "update " + ESQUEMA + ".actas set promedio = 1 where estado = 'ANULADA'"))
        .hasMessageContaining("Un acta anulada no se puede modificar");
  }

  @Test
  void siUnaMigracionFallaALaMitadElDisparadorVuelveAQuedarEncendido() {
    flyway("35").migrate();
    sembrarActas();
    Flyway conFalla =
        configurar(null)
            .locations("classpath:db/migration", "classpath:db/migracion-con-falla")
            .load();

    assertThatThrownBy(conFalla::migrate).isInstanceOf(FlywayException.class);

    assertThat(disparadorEncendido()).isTrue();
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "update " + ESQUEMA + ".actas set promedio = 1 where estado = 'FIRMADA'"))
        .hasMessageContaining("Un acta firmada es inmutable");
  }

  @Test
  void fueraDeUnaMigracionLosCallbacksNoApaganNada() {
    Flyway completa = flyway(null);
    completa.migrate();

    completa.info();
    completa.validate();
    completa.migrate(); // sin migraciones pendientes: beforeMigrate apaga y afterMigrate enciende

    assertThat(disparadorEncendido()).isTrue();
  }

  @Test
  void soloExistenLosTresCallbacksAlrededorDeMigrate() throws IOException {
    Path carpeta = Path.of("src/main/resources/db/migration");
    try (Stream<Path> archivos = Files.list(carpeta)) {
      assertThat(
              archivos
                  .map(archivo -> archivo.getFileName().toString())
                  .filter(nombre -> !nombre.startsWith("V")))
          .containsExactlyInAnyOrder(
              "beforeMigrate.sql", "afterMigrate.sql", "afterMigrateError.sql");
    }
  }

  private Flyway flyway(String objetivo) {
    return configurar(objetivo).load();
  }

  private org.flywaydb.core.api.configuration.FluentConfiguration configurar(String objetivo) {
    var configuracion =
        Flyway.configure()
            .dataSource(fuente)
            .schemas(ESQUEMA)
            .locations("classpath:db/migration")
            .cleanDisabled(true);
    return objetivo == null ? configuracion : configuracion.target(objetivo);
  }

  private void sembrarActas() {
    String e = ESQUEMA + ".";
    jdbc.update(
        "insert into "
            + e
            + "usuarios (nombre_completo, correo, contrasena_hash, rol)"
            + " values ('Vicerrectoría Ensayo', 'vice.ensayo@uniautonoma.edu.co', 'x', 'VICERRECTOR')");
    jdbc.update(
        "insert into "
            + e
            + "periodos_academicos (nombre, fecha_inicio, fecha_fin)"
            + " values ('2026-1', date '2026-02-01', date '2026-06-30')");
    String acta =
        "insert into "
            + e
            + "actas (id_docente, id_periodo, version, estado, promedio, estado_promedio, categoria,"
            + " umbral_compromisos, codigo_verificacion, hash_contenido, hash_pdf, pdf, fecha_firma,"
            + " fecha_creacion, version_bloqueo)"
            + " select u.id_usuario, p.id_periodo, ?, ?, ?, 'COMPLETO', 'ALTO', 3.8, ?, 'h', 'h',"
            + " decode('25504446', 'hex'), now(), now(), 0"
            + " from "
            + e
            + "usuarios u, "
            + e
            + "periodos_academicos p";
    jdbc.update(acta, 1, "FIRMADA", new java.math.BigDecimal("4.10"), "DOC-ENSAYO-1");
    jdbc.update(acta, 2, "ANULADA", new java.math.BigDecimal("2.90"), "DOC-ENSAYO-2");
  }

  private boolean disparadorEncendido() {
    return "O"
        .equals(
            jdbc.queryForObject(
                "select t.tgenabled from pg_trigger t join pg_class c on c.oid = t.tgrelid"
                    + " join pg_namespace n on n.oid = c.relnamespace"
                    + " where t.tgname = 'tr_actas_inmutables' and n.nspname = ?",
                String.class,
                ESQUEMA));
  }

  private java.util.List<String> tipos() {
    return jdbc.queryForList(
        "select estado || '=' || tipo from " + ESQUEMA + ".actas order by version", String.class);
  }
}
