package com.uniautonoma.docentia.service.analisis;

import java.util.Optional;

/**
 * Modelos que se pueden elegir en una solicitud. El local siempre está; el de lenguaje externo solo
 * si el proveedor por defecto ya es uno, o si el ambiente trae URL, modelo y clave. Nunca se cambia
 * de uno a otro sin que lo pidan.
 */
public class ModelosDisponibles {

  private final ModeloIA porDefecto;
  private final ModeloIA externo;
  private final String nombreExterno;

  /**
   * @param porDefecto el que usa una solicitud sin modelo
   * @param externo modelo de lenguaje disponible, o nulo
   * @param nombreExterno nombre del modelo externo en el proveedor (sin clave ni URL), o nulo
   */
  public ModelosDisponibles(ModeloIA porDefecto, ModeloIA externo, String nombreExterno) {
    this.porDefecto = porDefecto;
    this.externo = externo;
    this.nombreExterno = nombreExterno;
  }

  public ModeloIA porDefecto() {
    return porDefecto;
  }

  /** LOCAL o LLM. */
  public String codigoPorDefecto() {
    return "REGLAS".equals(porDefecto.tipo()) ? "LOCAL" : "LLM";
  }

  public Optional<ModeloIA> externo() {
    return Optional.ofNullable(externo);
  }

  public boolean hayExterno() {
    return externo != null;
  }

  public String nombreExterno() {
    return nombreExterno;
  }

  /** Si el modelo es el externo construido aparte (el por defecto es local). */
  boolean esExternoAparte(ModeloIA modelo) {
    return modelo != null && modelo == externo && modelo != porDefecto;
  }
}
