package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Compromiso;
import java.time.LocalDate;

public record CompromisoResponse(
    Long idCompromiso, String descripcion, LocalDate fechaLimite, String nombrePeriodo) {

  public static CompromisoResponse desde(Compromiso compromiso) {
    return new CompromisoResponse(
        compromiso.getIdCompromiso(),
        compromiso.getDescripcion(),
        compromiso.getFechaLimite(),
        compromiso.getPeriodo().getNombre());
  }
}
