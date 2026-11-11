package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.FuenteComentarios;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Pedido de análisis.
 *
 * @param fuente CSV (por defecto, si se omite) o FORMULARIO
 * @param modelo LOCAL (reglas, sin tokens) o LLM (modelo de lenguaje externo, consume tokens); nulo
 *     usa el configurado por defecto
 * @param motivo obligatorio si el docente ya tiene un análisis en el periodo: pedir otro es
 *     regenerar, con el mismo motivo y enfriamiento que {@code /regeneracion}
 */
public record SolicitudAnalisisRequest(
    @NotNull(message = "El docente es obligatorio") Long docenteId,
    @NotNull(message = "El periodo es obligatorio") Long periodoId,
    FuenteComentarios fuente,
    @Pattern(regexp = "LOCAL|LLM", message = "El modelo debe ser LOCAL o LLM") String modelo,
    @Size(min = 10, max = 300, message = "El motivo debe tener entre 10 y 300 caracteres")
        String motivo) {

  public SolicitudAnalisisRequest(Long docenteId, Long periodoId) {
    this(docenteId, periodoId, null, null, null);
  }

  public SolicitudAnalisisRequest(Long docenteId, Long periodoId, FuenteComentarios fuente) {
    this(docenteId, periodoId, fuente, null, null);
  }

  public SolicitudAnalisisRequest(
      Long docenteId, Long periodoId, FuenteComentarios fuente, String modelo) {
    this(docenteId, periodoId, fuente, modelo, null);
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
