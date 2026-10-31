package com.uniautonoma.docentia.entity;

import java.math.BigDecimal;
import java.util.function.Function;

/** Cada aspecto calificado, visto como dimensión para comparar las tres fuentes entre sí. */
public enum Dimension {
  DOMINIO_DISCIPLINAR("Dominio disciplinar", NotasAspectos::dominioDisciplinar),
  PLANEACION_ACADEMICA("Planeación académica", NotasAspectos::planeacionAcademica),
  METODOLOGIA_ENSENANZA("Metodología de enseñanza", NotasAspectos::metodologiaEnsenanza),
  EVALUACION_APRENDIZAJE("Evaluación del aprendizaje", NotasAspectos::evaluacionAprendizaje),
  COMPROMISO_INSTITUCIONAL("Compromiso institucional", NotasAspectos::compromisoInstitucional);

  private final String nombre;
  private final Function<NotasAspectos, BigDecimal> lectorNota;

  Dimension(String nombre, Function<NotasAspectos, BigDecimal> lectorNota) {
    this.nombre = nombre;
    this.lectorNota = lectorNota;
  }

  public String getNombre() {
    return nombre;
  }

  public BigDecimal notaEn(NotasAspectos notas) {
    return lectorNota.apply(notas);
  }
}
