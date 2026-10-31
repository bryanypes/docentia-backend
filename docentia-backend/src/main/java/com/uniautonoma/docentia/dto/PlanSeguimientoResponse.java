package com.uniautonoma.docentia.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Compromisos del periodo anterior al activo.
 *
 * @param periodoEvaluado periodo del que vienen los compromisos, o nulo si no hay anterior
 */
public record PlanSeguimientoResponse(
    PeriodoResumenResponse periodoEvaluado, List<CompromisoSeguimiento> compromisos) {

  public enum EstadoPlan {
    PENDIENTE,
    CUMPLIDO,
    VENCIDO
  }

  public record CompromisoSeguimiento(
      Long idCompromiso,
      String descripcion,
      LocalDate fechaLimite,
      EstadoPlan estado,
      String observacion,
      List<CambioEstado> historial) {}

  public record CambioEstado(
      String estado, String observacion, String nombreUsuario, LocalDateTime fecha) {}
}
