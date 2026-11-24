package com.uniautonoma.docentia.service.heteroevaluacion;

import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.service.PeriodoAcademicoService;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consolida la nota de estudiantes con todas las respuestas disponibles. También repara respuestas
 * anteriores si el servicio se reinició o la consolidación previa no llegó a ejecutarse.
 */
@Component
public class ConsolidadorHeteroevaluacion {

  private final PeriodoAcademicoService periodoService;
  private final HeteroevaluacionService heteroevaluacion;
  private final UsuarioRepository usuarioRepository;
  private final JdbcTemplate jdbcTemplate;

  public ConsolidadorHeteroevaluacion(
      PeriodoAcademicoService periodoService,
      HeteroevaluacionService heteroevaluacion,
      UsuarioRepository usuarioRepository,
      JdbcTemplate jdbcTemplate) {
    this.periodoService = periodoService;
    this.heteroevaluacion = heteroevaluacion;
    this.usuarioRepository = usuarioRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Scheduled(
      initialDelayString = "${docentia.heteroevaluacion.consolidacion.espera-inicial-ms:60000}",
      fixedDelayString = "${docentia.heteroevaluacion.consolidacion.intervalo-ms:900000}")
  @Transactional
  public void consolidar() {
    periodoService
        .buscarPeriodoActivo()
        .ifPresent(
            periodo -> {
              List<Long> docentes =
                  jdbcTemplate.queryForList(
                      "select distinct docente_id from respuestas_heteroevaluacion"
                          + " where periodo_id = ?",
                      Long.class,
                      periodo.getIdPeriodo());
              docentes.forEach(
                  id ->
                      heteroevaluacion.recalcular(usuarioRepository.getReferenceById(id), periodo));
            });
  }
}
