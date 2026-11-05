package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.FuenteComentarios;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Pedido de análisis.
 *
 * @param fuente CSV (por defecto, si se omite) o FORMULARIO
 * @param modelo LOCAL para usar el modelo por reglas aunque haya un proveedor externo configurado
 *     (por ejemplo, cuando se agotó el presupuesto); nulo usa el proveedor configurado
 */
public record SolicitudAnalisisRequest(
    @NotNull(message = "El docente es obligatorio") Long docenteId,
    @NotNull(message = "El periodo es obligatorio") Long periodoId,
    FuenteComentarios fuente,
    @Pattern(regexp = "LOCAL", message = "El modelo solo puede ser LOCAL") String modelo) {

  public SolicitudAnalisisRequest(Long docenteId, Long periodoId) {
    this(docenteId, periodoId, null, null);
  }

  public SolicitudAnalisisRequest(Long docenteId, Long periodoId, FuenteComentarios fuente) {
    this(docenteId, periodoId, fuente, null);
  }

  public FuenteComentarios fuenteOPorDefecto() {
    return fuente == null ? FuenteComentarios.CSV : fuente;
  }

  public boolean soloLocal() {
    return "LOCAL".equals(modelo);
  }
}
