package com.uniautonoma.docentia.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Clase;
import com.uniautonoma.docentia.entity.Curso;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.service.heteroevaluacion.HeteroevaluacionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
 * El recálculo por envío y la tarea periódica pueden coincidir sobre el mismo docente: el bloqueo
 * por docente y periodo los pone en fila. Sin él, los dos crearían la nota a la vez y uno chocaría
 * con la restricción única. Corre sin la transacción de la prueba porque cada hilo necesita ver
 * datos confirmados; al final borra lo que creó.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class HeteroevaluacionConcurrenciaTest extends PruebaIntegracion {

  private static final int DOCENTES = 8;
  private static final int RESPUESTAS = 5;
  private static final String PERIODO = "H-2026";

  @Autowired private EscenarioPrueba escenario;
  @Autowired private HeteroevaluacionService heteroevaluacion;
  @Autowired private TransactionTemplate transaccion;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void dosRecalculosAlMismoTiempoDejanUnaSolaNotaConsistente() throws Exception {
    List<Long> idsDocentes = new ArrayList<>();
    Long idPeriodo =
        transaccion.execute(
            estado -> {
              Facultad facultad = escenario.crearFacultad("Facultad hetero concurrencia");
              Programa programa = escenario.crearPrograma("Programa hetero concurrencia", facultad);
              PeriodoAcademico periodo =
                  escenario.crearPeriodoCerrado(PERIODO, LocalDate.of(2025, 2, 3));
              Curso curso = escenario.crearCurso("HC-1", "Curso concurrencia", programa);
              Long descriptor =
                  jdbcTemplate.queryForObject(
                      "select d.id from descriptores_evaluacion d"
                          + " join descriptor_actor a on a.descriptor_id = d.id"
                          + " where a.tipo = 'HETERO' order by d.orden limit 1",
                      Long.class);
              for (int i = 0; i < DOCENTES; i++) {
                Usuario docente =
                    escenario.crearDocente("Docente Hetero Concurrencia " + i, programa);
                Clase clase = escenario.crearClase(periodo, curso, "G" + i, docente);
                Long asignacion =
                    jdbcTemplate.queryForObject(
                        "select id from clases_docente where clase_id = ?",
                        Long.class,
                        clase.getId());
                for (int r = 0; r < RESPUESTAS; r++) {
                  UUID respuesta =
                      jdbcTemplate.queryForObject(
                          "insert into respuestas_heteroevaluacion"
                              + " (asignacion_id, docente_id, periodo_id, instrumento_version)"
                              + " values (?, ?, ?, 'RES074-2026-ANEXO1') returning id",
                          UUID.class,
                          asignacion,
                          docente.getIdUsuario(),
                          periodo.getIdPeriodo());
                  jdbcTemplate.update(
                      "insert into respuestas_descriptor_hetero"
                          + " (respuesta_id, descriptor_id, valor) values (?, ?, 4)",
                      respuesta,
                      descriptor);
                }
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
                    heteroevaluacion.recalcular(idDocente, idPeriodo);
                    return null;
                  }));
        }
      }
      // Ninguno de los dos recálculos falla: el segundo espera al primero y no escribe nada.
      for (Future<?> resultado : resultados) {
        resultado.get(30, TimeUnit.SECONDS);
      }
    } finally {
      hilos.shutdownNow();
    }

    for (Long idDocente : idsDocentes) {
      List<BigDecimal> notas =
          jdbcTemplate.queryForList(
              "select nota_fuente from calificaciones"
                  + " where id_docente = ? and id_periodo = ? and fuente = 'ESTUDIANTES'",
              BigDecimal.class,
              idDocente,
              idPeriodo);
      assertThat(notas).hasSize(1);
      assertThat(notas.get(0)).isEqualByComparingTo("4");
    }
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from correcciones_calificacion c"
                    + " join calificaciones k on k.id_calificacion = c.id_calificacion"
                    + " where k.id_periodo = ?",
                Long.class,
                idPeriodo))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from promedios_docente where id_periodo = ?",
                Integer.class,
                idPeriodo))
        .isEqualTo(DOCENTES);
  }

  @AfterEach
  void borrarLoCreado() {
    String periodo =
        "(select id_periodo from periodos_academicos where nombre = '" + PERIODO + "')";
    jdbcTemplate.update(
        "delete from respuestas_descriptor_hetero where respuesta_id in"
            + " (select id from respuestas_heteroevaluacion where periodo_id in "
            + periodo
            + ")");
    jdbcTemplate.update("delete from respuestas_heteroevaluacion where periodo_id in " + periodo);
    jdbcTemplate.update(
        "delete from correcciones_calificacion where id_calificacion in"
            + " (select id_calificacion from calificaciones where id_periodo in "
            + periodo
            + ")");
    jdbcTemplate.update("delete from promedios_docente where id_periodo in " + periodo);
    jdbcTemplate.update("delete from calificaciones where id_periodo in " + periodo);
    jdbcTemplate.update(
        "delete from clases_docente where clase_id in"
            + " (select id from clases where id_periodo in "
            + periodo
            + ")");
    jdbcTemplate.update("delete from clases where id_periodo in " + periodo);
    jdbcTemplate.update("delete from cursos where codigo = 'HC-1'");
    jdbcTemplate.update("delete from periodos_academicos where nombre = '" + PERIODO + "'");
    jdbcTemplate.update(
        "delete from usuarios where nombre_completo like 'Docente Hetero Concurrencia %'");
    jdbcTemplate.update("delete from programas where nombre = 'Programa hetero concurrencia'");
    jdbcTemplate.update("delete from facultades where nombre = 'Facultad hetero concurrencia'");
  }
}
