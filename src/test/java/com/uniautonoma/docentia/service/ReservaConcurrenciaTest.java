package com.uniautonoma.docentia.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.FranjaHorariaRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * F2: varios docentes reservan la misma franja exactamente al mismo tiempo. Corre sin la
 * transacción de la prueba porque cada hilo necesita ver lo que confirman los demás; al final borra
 * lo que creó.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReservaConcurrenciaTest extends PruebaIntegracion {

  private static final int DOCENTES = 8;
  private static final String MARCA = "Concurrencia Reserva";

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CitaService citaService;
  @Autowired private FranjaHorariaRepository franjaRepository;
  @Autowired private TransactionTemplate transaccion;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void variosDocentesSobreLaMismaFranjaDejanExactamenteUnaReserva() throws Exception {
    List<UsuarioSesion> docentes = new ArrayList<>();
    Long idFranja =
        transaccion.execute(
            estado -> {
              Facultad facultad = escenario.crearFacultad("Facultad " + MARCA);
              Programa programa = escenario.crearPrograma("Programa " + MARCA, facultad);
              Usuario coordinador = escenario.crearCoordinador("Coordinador " + MARCA, facultad);
              PeriodoAcademico periodo =
                  escenario.abrirEtapa(
                      escenario.crearPeriodoActivo("CR-2026"),
                      TipoEtapa.AGENDA_CITAS,
                      BaseDatosPruebaConfig.HOY,
                      BaseDatosPruebaConfig.HOY);
              for (int i = 0; i < DOCENTES; i++) {
                docentes.add(
                    UsuarioSesion.desde(
                        escenario.crearDocente("Docente " + MARCA + " " + i, programa)));
              }
              return franjaRepository
                  .saveAndFlush(
                      new FranjaHoraria(
                          coordinador,
                          periodo,
                          BaseDatosPruebaConfig.HOY.plusDays(7),
                          LocalTime.of(8, 0)))
                  .getIdFranja();
            });

    CyclicBarrier salida = new CyclicBarrier(DOCENTES);
    ExecutorService hilos = Executors.newFixedThreadPool(DOCENTES);
    List<Future<String>> resultados = new ArrayList<>();
    try {
      for (UsuarioSesion docente : docentes) {
        resultados.add(
            hilos.submit(
                () -> {
                  salida.await(10, TimeUnit.SECONDS);
                  try {
                    citaService.reservar(idFranja, docente);
                    return "RESERVADA";
                  } catch (ReglaNegocioException conflicto) {
                    return "409";
                  }
                }));
      }
      List<String> respuestas = new ArrayList<>();
      for (Future<String> resultado : resultados) {
        respuestas.add(resultado.get(60, TimeUnit.SECONDS));
      }

      assertThat(respuestas).containsOnlyOnce("RESERVADA");
      assertThat(respuestas.stream().filter("409"::equals)).hasSize(DOCENTES - 1);
    } finally {
      hilos.shutdownNow();
    }
    Integer activas =
        jdbcTemplate.queryForObject(
            "select count(*) from citas where id_franja = ? and estado = 'ACTIVA'",
            Integer.class,
            idFranja);
    assertThat(activas).isEqualTo(1);
  }

  @AfterEach
  void borrarLoCreado() {
    jdbcTemplate.update(
        "delete from citas where id_periodo in"
            + " (select id_periodo from periodos_academicos where nombre = 'CR-2026')");
    jdbcTemplate.update(
        "delete from franjas_horarias where id_periodo in"
            + " (select id_periodo from periodos_academicos where nombre = 'CR-2026')");
    jdbcTemplate.update("delete from periodos_academicos where nombre = 'CR-2026'");
    jdbcTemplate.update("delete from usuarios where nombre_completo like '%" + MARCA + "%'");
    jdbcTemplate.update("delete from programas where nombre = 'Programa " + MARCA + "'");
    jdbcTemplate.update("delete from facultades where nombre = 'Facultad " + MARCA + "'");
  }
}
