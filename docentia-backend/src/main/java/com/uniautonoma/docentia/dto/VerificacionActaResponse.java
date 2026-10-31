package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Acta;
import com.uniautonoma.docentia.entity.EstadoActa;
import java.time.LocalDateTime;

/** Lo que cualquiera puede comprobar con el código del acta. No incluye notas ni categoría. */
public record VerificacionActaResponse(
    String nombreDocente,
    String periodo,
    int version,
    EstadoActa estado,
    LocalDateTime fechaFirma,
    String hashContenido,
    String hashPdf) {

  public static VerificacionActaResponse desde(Acta acta) {
    return new VerificacionActaResponse(
        acta.getDocente().getNombreCompleto(),
        acta.getPeriodo().getNombre(),
        acta.getVersion(),
        acta.getEstado(),
        acta.getFechaFirma(),
        acta.getHashContenido(),
        acta.getHashPdf());
  }
}
