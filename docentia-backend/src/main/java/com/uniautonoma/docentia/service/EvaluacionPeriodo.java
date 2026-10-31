package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.entity.Dimension;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.NotasAspectos;
import com.uniautonoma.docentia.entity.PesosFuentes;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Lo que las fuentes le calificaron a un docente en un periodo y los pesos con que se pondera. Las
 * fuentes que no han calificado simplemente no están en el mapa.
 */
public record EvaluacionPeriodo(
    Map<FuenteEvaluacion, NotasAspectos> notasPorFuente, PesosFuentes pesos) {

  public Map<FuenteEvaluacion, BigDecimal> notasDeFuentes() {
    return leerNotas(NotasAspectos::promedio);
  }

  public Map<FuenteEvaluacion, BigDecimal> notasDeDimension(Dimension dimension) {
    return leerNotas(dimension::notaEn);
  }

  private Map<FuenteEvaluacion, BigDecimal> leerNotas(Function<NotasAspectos, BigDecimal> lector) {
    Map<FuenteEvaluacion, BigDecimal> notas = new EnumMap<>(FuenteEvaluacion.class);
    notasPorFuente.forEach((fuente, aspectos) -> notas.put(fuente, lector.apply(aspectos)));
    return notas;
  }
}
