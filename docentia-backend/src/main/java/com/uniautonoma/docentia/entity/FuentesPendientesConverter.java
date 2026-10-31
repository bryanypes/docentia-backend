package com.uniautonoma.docentia.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Guarda la lista de fuentes pendientes como texto separado por comas; vacía se guarda nula. */
@Converter
public class FuentesPendientesConverter
    implements AttributeConverter<List<FuenteEvaluacion>, String> {

  private static final String SEPARADOR = ",";

  @Override
  public String convertToDatabaseColumn(List<FuenteEvaluacion> fuentes) {
    if (fuentes == null || fuentes.isEmpty()) {
      return null;
    }
    return fuentes.stream().map(FuenteEvaluacion::name).collect(Collectors.joining(SEPARADOR));
  }

  @Override
  public List<FuenteEvaluacion> convertToEntityAttribute(String columna) {
    if (columna == null || columna.isBlank()) {
      return List.of();
    }
    return Arrays.stream(columna.split(SEPARADOR)).map(FuenteEvaluacion::valueOf).toList();
  }
}
