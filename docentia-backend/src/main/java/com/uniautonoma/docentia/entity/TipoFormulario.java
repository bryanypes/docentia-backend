package com.uniautonoma.docentia.entity;

/** Formularios del Anexo 1 que se responden en DOCENTIA; la heteroevaluación llega por CSV. */
public enum TipoFormulario {
  AUTO(FuenteEvaluacion.AUTOEVALUACION),
  COEVAL(FuenteEvaluacion.COORDINACION);

  private final FuenteEvaluacion fuente;

  TipoFormulario(FuenteEvaluacion fuente) {
    this.fuente = fuente;
  }

  public FuenteEvaluacion getFuente() {
    return fuente;
  }
}
