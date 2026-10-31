package com.uniautonoma.docentia.entity;

/**
 * Categorías de desempeño de la Resolución 074 de 2026, con la acción institucional de cada una.
 * Las acciones son informativas: el sistema nunca las ejecuta.
 */
public enum CategoriaDesempeno {
  MUY_ALTO("Muy alto desempeño", "Reconocimiento público"),
  ALTO("Alto desempeño", "Felicitación institucional"),
  ACEPTABLE("Desempeño aceptable", "Continuidad y recomendaciones"),
  BAJO(
      "Bajo desempeño", "Plan de mejoramiento, período adicional (máximo 1 año) o reclasificación"),
  MUY_BAJO(
      "Muy bajo desempeño",
      "Alerta informativa para Vicerrectoría — la institución evalúa el proceso conforme a su"
          + " normativa");

  private final String etiqueta;
  private final String accionInstitucional;

  CategoriaDesempeno(String etiqueta, String accionInstitucional) {
    this.etiqueta = etiqueta;
    this.accionInstitucional = accionInstitucional;
  }

  public String getEtiqueta() {
    return etiqueta;
  }

  public String getAccionInstitucional() {
    return accionInstitucional;
  }
}
