package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.NotasAspectos;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CalificacionResponse(
    Long idCalificacion,
    Long idDocente,
    String nombrePeriodo,
    FuenteEvaluacion fuente,
    BigDecimal dominioDisciplinar,
    BigDecimal planeacionAcademica,
    BigDecimal metodologiaEnsenanza,
    BigDecimal evaluacionAprendizaje,
    BigDecimal compromisoInstitucional,
    String observacion,
    LocalDateTime fechaActualizacion) {

  public static CalificacionResponse desde(Calificacion calificacion) {
    NotasAspectos notas = calificacion.getNotas();
    return new CalificacionResponse(
        calificacion.getIdCalificacion(),
        calificacion.getDocente().getIdUsuario(),
        calificacion.getPeriodo().getNombre(),
        calificacion.getFuente(),
        notas.dominioDisciplinar(),
        notas.planeacionAcademica(),
        notas.metodologiaEnsenanza(),
        notas.evaluacionAprendizaje(),
        notas.compromisoInstitucional(),
        calificacion.getObservacion(),
        calificacion.getFechaActualizacion());
  }
}
