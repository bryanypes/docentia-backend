package com.uniautonoma.docentia.exception;

import com.uniautonoma.docentia.entity.TipoEtapa;
import java.time.LocalDate;

/**
 * Se intentó una acción fuera de la ventana de su etapa. Lleva la etapa y sus fechas para que la
 * respuesta le diga al usuario cuándo sí puede hacerlo; las fechas son nulas si la etapa no está
 * programada en el periodo.
 */
public class EtapaCerradaException extends RuntimeException {

  private final TipoEtapa etapa;
  private final LocalDate fechaApertura;
  private final LocalDate fechaCierre;
  private final LocalDate fechaActual;

  public EtapaCerradaException(
      String mensaje,
      TipoEtapa etapa,
      LocalDate fechaApertura,
      LocalDate fechaCierre,
      LocalDate fechaActual) {
    super(mensaje);
    this.etapa = etapa;
    this.fechaApertura = fechaApertura;
    this.fechaCierre = fechaCierre;
    this.fechaActual = fechaActual;
  }

  public TipoEtapa getEtapa() {
    return etapa;
  }

  public LocalDate getFechaApertura() {
    return fechaApertura;
  }

  public LocalDate getFechaCierre() {
    return fechaCierre;
  }

  public LocalDate getFechaActual() {
    return fechaActual;
  }
}
