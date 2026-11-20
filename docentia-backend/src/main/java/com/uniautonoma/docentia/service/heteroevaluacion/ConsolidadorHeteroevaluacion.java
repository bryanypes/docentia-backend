package com.uniautonoma.docentia.service.heteroevaluacion;

import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.service.PeriodoAcademicoService;
import com.uniautonoma.docentia.service.VentanaEtapaService;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Al cerrar la recolección deja la nota de estudiantes con todas las respuestas: mientras estuvo
 * abierta solo se actualizó por bloques. Recalcular de nuevo no cambia nada si la nota ya está al
 * día.
 */
@Component
public class ConsolidadorHeteroevaluacion {

  private final PeriodoAcademicoService periodoService;
  private final VentanaEtapaService ventanas;
  private final HeteroevaluacionService heteroevaluacion;
  private final UsuarioRepository usuarioRepository;
  private final JdbcTemplate jdbcTemplate;

  public ConsolidadorHeteroevaluacion(
      PeriodoAcademicoService periodoService,
      VentanaEtapaService ventanas,
      HeteroevaluacionService heteroevaluacion,
      UsuarioRepository usuarioRepository,
      JdbcTemplate jdbcTemplate) {
    this.periodoService = periodoService;
    this.ventanas = ventanas;
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
        .filter(periodo -> ventanas.haTerminado(periodo, TipoEtapa.RECOLECCION_CALIFICACIONES))
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
