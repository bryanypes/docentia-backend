package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.CorreccionCalificacion;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.Usuario;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Una corrección del historial. notaAnterior y notaNueva son la nota de la fuente (promedio de sus
 * cinco aspectos); los promedios son el ponderado del docente antes y después.
 */
public record CorreccionCalificacionResponse(
    Long idCorreccion,
    Long idPeriodo,
    String nombrePeriodo,
    FuenteEvaluacion fuente,
    BigDecimal notaAnterior,
    BigDecimal notaNueva,
    PromedioPonderadoResponse promedioAnterior,
    PromedioPonderadoResponse promedioNuevo,
    Long idUsuario,
    String nombreUsuario,
    String motivo,
    LocalDateTime fechaCorreccion) {

  public static CorreccionCalificacionResponse desde(CorreccionCalificacion correccion) {
    Usuario usuario = correccion.getUsuario();
    return new CorreccionCalificacionResponse(
        correccion.getIdCorreccion(),
        correccion.getPeriodo().getIdPeriodo(),
        correccion.getPeriodo().getNombre(),
        correccion.getFuente(),
        correccion.getNotaAnterior(),
        correccion.getNotaNueva(),
        // Las fuentes pendientes no se guardan en el historial: el estado y el valor bastan.
        new PromedioPonderadoResponse(
            correccion.getEstadoPromedioAnterior(), correccion.getPromedioAnterior(), List.of()),
        new PromedioPonderadoResponse(
            correccion.getEstadoPromedioNuevo(), correccion.getPromedioNuevo(), List.of()),
        usuario != null ? usuario.getIdUsuario() : null,
        usuario != null ? usuario.getNombreCompleto() : null,
        correccion.getMotivo(),
        correccion.getFechaCorreccion());
  }
}
