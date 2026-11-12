package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.TipoActa;
import jakarta.validation.constraints.NotNull;

/**
 * Docente para el que se abre el acta del periodo activo.
 *
 * @param tipo opcional; sin él queda de retroalimentación, o de compromiso si la nota lo exige
 */
public record NuevaActaRequest(
    @NotNull(message = "El docente es obligatorio") Long idDocente, TipoActa tipo) {

  public NuevaActaRequest(Long idDocente) {
    this(idDocente, null);
  }
}
