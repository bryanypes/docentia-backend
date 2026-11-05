package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.FuenteComentarios;
import jakarta.validation.constraints.NotNull;

/**
 * Pedido de análisis.
 *
 * @param fuente CSV (por defecto, si se omite) o FORMULARIO
 */
public record SolicitudAnalisisRequest(
    @NotNull(message = "El docente es obligatorio") Long docenteId,
    @NotNull(message = "El periodo es obligatorio") Long periodoId,
    FuenteComentarios fuente) {

  public SolicitudAnalisisRequest(Long docenteId, Long periodoId) {
    this(docenteId, periodoId, null);
  }

  public FuenteComentarios fuenteOPorDefecto() {
    return fuente == null ? FuenteComentarios.CSV : fuente;
  }
}
