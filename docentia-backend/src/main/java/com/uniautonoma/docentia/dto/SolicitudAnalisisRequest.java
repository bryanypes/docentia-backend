package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.FuenteComentarios;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Pedido de análisis.
 *
 * @param fuente CSV (por defecto, si se omite) o FORMULARIO
 * @param modelo LOCAL (reglas, sin tokens) o LLM (modelo de lenguaje externo, consume tokens); nulo
 *     usa el configurado por defecto
 */
public record SolicitudAnalisisRequest(
    @NotNull(message = "El docente es obligatorio") Long docenteId,
    @NotNull(message = "El periodo es obligatorio") Long periodoId,
    FuenteComentarios fuente,
    @Pattern(regexp = "LOCAL|LLM", message = "El modelo debe ser LOCAL o LLM") String modelo) {

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

  public boolean pideLlm() {
    return "LLM".equals(modelo);
  }
}
