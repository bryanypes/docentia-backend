package com.uniautonoma.docentia.entity;

/** Formularios del Anexo 1 que se responden en DOCENTIA. */
public enum TipoFormulario {
  AUTO(FuenteEvaluacion.AUTOEVALUACION),
  COEVAL(FuenteEvaluacion.COORDINACION),
  HETERO(FuenteEvaluacion.ESTUDIANTES);

  private final FuenteEvaluacion fuente;

  TipoFormulario(FuenteEvaluacion fuente) {
    this.fuente = fuente;
  }

  public FuenteEvaluacion getFuente() {
    return fuente;
  }
}
