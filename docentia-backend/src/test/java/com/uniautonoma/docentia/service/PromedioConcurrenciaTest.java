package com.uniautonoma.docentia.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Usuario;
import java.time.LocalDate;
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
 * Dos recálculos simultáneos del mismo docente no pueden chocar. Corre sin la transacción de la
 * prueba porque cada hilo necesita ver datos confirmados; al final borra lo que creó.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PromedioConcurrenciaTest extends PruebaIntegracion {

  private static final int DOCENTES = 10;

  @Autowired private EscenarioPrueba escenario;
  @Autowired private PromedioService promedioService;
  @Autowired private TransactionTemplate transaccion;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void dosRecalculosAlMismoTiempoDejanUnSoloPromedioPorDocente() throws Exception {
    List<Long> idsDocentes = new ArrayList<>();
    Long idPeriodo =
        transaccion.execute(
            estado -> {
              Facultad facultad = escenario.crearFacultad("Facultad concurrencia");
              Programa programa = escenario.crearPrograma("Programa concurrencia", facultad);
              PeriodoAcademico periodo =
                  escenario.crearPeriodoCerrado("C-2026", LocalDate.of(2025, 2, 3));
              for (int i = 0; i < DOCENTES; i++) {
                Usuario docente = escenario.crearDocente("Docente Concurrencia " + i, programa);
                escenario.guardarCalificacionSinPromedio(
                    docente, periodo, FuenteEvaluacion.COORDINACION, "4.0");
                idsDocentes.add(docente.getIdUsuario());
              }
              return periodo.getIdPeriodo();
            });

    ExecutorService hilos = Executors.newFixedThreadPool(2);
    List<Future<?>> resultados = new ArrayList<>();
    try {
      for (Long idDocente : idsDocentes) {
        CyclicBarrier salida = new CyclicBarrier(2);
        for (int hilo = 0; hilo < 2; hilo++) {
          resultados.add(
              hilos.submit(
                  () -> {
                    salida.await(5, TimeUnit.SECONDS);
                    return promedioService.recalcular(idDocente, idPeriodo);
                  }));
        }
      }
      for (Future<?> resultado : resultados) {
        resultado.get(30, TimeUnit.SECONDS);
      }
    } finally {
      hilos.shutdownNow();
    }

    Integer filas =
        jdbcTemplate.queryForObject(
            "select count(*) from promedios_docente where id_periodo = ?",
            Integer.class,
            idPeriodo);
    assertThat(filas).isEqualTo(DOCENTES);
  }

  @AfterEach
  void borrarLoCreado() {
    jdbcTemplate.update(
        "delete from promedios_docente where id_periodo in"
            + " (select id_periodo from periodos_academicos where nombre = 'C-2026')");
    jdbcTemplate.update(
        "delete from calificaciones where id_periodo in"
            + " (select id_periodo from periodos_academicos where nombre = 'C-2026')");
    jdbcTemplate.update("delete from periodos_academicos where nombre = 'C-2026'");
    jdbcTemplate.update("delete from usuarios where nombre_completo like 'Docente Concurrencia %'");
    jdbcTemplate.update("delete from programas where nombre = 'Programa concurrencia'");
    jdbcTemplate.update("delete from facultades where nombre = 'Facultad concurrencia'");
  }
}
