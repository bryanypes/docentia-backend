package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.Size;

/**
 * Firma del acta.
 *
 * @param confirmacion verdadero si quien firma confirmó que es irreversible
 * @param trazo PNG de la firma dibujada en la página, como data URL ({@code
 *     data:image/png;base64,})
 */
public record FirmaActaRequest(
    boolean confirmacion,
    @Size(max = 400_000, message = "El trazo de la firma es demasiado grande") String trazo) {

  public FirmaActaRequest(boolean confirmacion) {
    this(confirmacion, null);
  }
}
