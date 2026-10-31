package com.uniautonoma.docentia.entity;

import java.util.Arrays;
import java.util.Optional;

/** Clase de hallazgo. El código es como lo devuelve el modelo en el JSON. */
public enum TipoHallazgo {
  FORTALEZA("fortaleza"),
  OPORTUNIDAD_DE_MEJORA("oportunidad_de_mejora");

  private final String codigo;

  TipoHallazgo(String codigo) {
    this.codigo = codigo;
  }

  public String getCodigo() {
    return codigo;
  }

  public static Optional<TipoHallazgo> desdeCodigo(String codigo) {
    return Arrays.stream(values()).filter(tipo -> tipo.codigo.equals(codigo)).findFirst();
  }
}
