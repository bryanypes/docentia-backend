package com.uniautonoma.docentia.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Varias franjas a la vez.
 *
 * @param accion ABRIR (usa franjas) o CERRAR (usa ids)
 * @param franjas franjas a abrir, como en POST /franjas
 * @param ids franjas a cerrar
 */
public record LoteFranjasRequest(
    @NotNull(message = "La acción es obligatoria") AccionLote accion,
    @Size(max = 200, message = "Máximo 200 franjas por lote") List<@Valid FranjaRequest> franjas,
    @Size(max = 200, message = "Máximo 200 franjas por lote") List<Long> ids) {

  public enum AccionLote {
    ABRIR,
    CERRAR
  }
}
