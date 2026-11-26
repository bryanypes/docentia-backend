package com.uniautonoma.docentia.service.heteroevaluacion;

import com.uniautonoma.docentia.service.PeriodoAcademicoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Revisa la nota de estudiantes de todos los docentes del periodo activo, abierta o cerrada la
 * recolección, con la misma regla que el envío: abierta, solo en los puntos de publicación;
 * cerrada, con todas las respuestas. Así consolida al cerrar y repara una nota que no se alcanzó a
 * recalcular (por ejemplo, tras un reinicio). Es idempotente: si nada cambió, no escribe.
 *
 * <p>Cada docente va en su propia transacción, con el mismo bloqueo que el envío: la tarea no
 * retiene a los estudiantes que envían mientras corre, y un fallo en un docente no detiene a los
 * demás.
 */
@Component
public class ConsolidadorHeteroevaluacion {

  private static final Logger LOG = LoggerFactory.getLogger(ConsolidadorHeteroevaluacion.class);

  private final PeriodoAcademicoService periodoService;
  private final HeteroevaluacionService heteroevaluacion;
  private final JdbcTemplate jdbcTemplate;

  public ConsolidadorHeteroevaluacion(
      PeriodoAcademicoService periodoService,
      HeteroevaluacionService heteroevaluacion,
      JdbcTemplate jdbcTemplate) {
    this.periodoService = periodoService;
    this.heteroevaluacion = heteroevaluacion;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Scheduled(
      initialDelayString = "${docentia.heteroevaluacion.consolidacion.espera-inicial-ms:60000}",
      fixedDelayString = "${docentia.heteroevaluacion.consolidacion.intervalo-ms:900000}")
  public void consolidar() {
    periodoService
        .buscarPeriodoActivo()
        .ifPresent(
            periodo ->
                jdbcTemplate
                    .queryForList(
                        "select distinct docente_id from respuestas_heteroevaluacion"
                            + " where periodo_id = ?",
                        Long.class,
                        periodo.getIdPeriodo())
                    .forEach(
                        idDocente -> {
                          try {
                            heteroevaluacion.recalcular(idDocente, periodo.getIdPeriodo());
                          } catch (RuntimeException fallo) {
                            LOG.error(
                                "No se pudo revisar la nota de estudiantes del docente {}: {}",
                                idDocente,
                                fallo.getClass().getName());
                          }
                        }));
  }
}
