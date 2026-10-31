package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.exception.EtapaCerradaException;
import java.time.LocalDate;
import java.util.Map;

/** Cuerpo del 409 por etapa cerrada: el error de siempre más la etapa y su ventana. */
public record EtapaCerradaResponse(
    String mensaje,
    Map<String, String> errores,
    TipoEtapa etapa,
    String nombreEtapa,
    LocalDate fechaApertura,
    LocalDate fechaCierre,
    LocalDate fechaActual) {

  public static EtapaCerradaResponse desde(EtapaCerradaException excepcion) {
    return new EtapaCerradaResponse(
        excepcion.getMessage(),
        Map.of(),
        excepcion.getEtapa(),
        excepcion.getEtapa().getNombre(),
        excepcion.getFechaApertura(),
        excepcion.getFechaCierre(),
        excepcion.getFechaActual());
  }
}
