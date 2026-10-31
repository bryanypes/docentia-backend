package com.uniautonoma.docentia.service.importacion;

import java.util.List;
import java.util.Map;

/**
 * Contenido de un CSV ya decodificado.
 *
 * @param columnas encabezados en el orden del archivo
 * @param filas filas de datos; las completamente vacías no se incluyen
 * @param codificacion UTF-8 o ISO-8859-1
 * @param delimitador coma o punto y coma
 */
public record TablaCsv(
    List<String> columnas, List<Fila> filas, String codificacion, char delimitador) {

  /**
   * Una fila de datos.
   *
   * @param numero número de fila tal como se ve en Excel: el encabezado es la fila 1
   * @param valores valor de cada columna, recortado; vacío si la fila trae menos valores
   * @param valoresDeMas cuántos valores trae la fila por encima de las columnas del encabezado
   */
  public record Fila(int numero, Map<String, String> valores, int valoresDeMas) {

    public String valor(String columna) {
      return columna == null ? "" : valores.getOrDefault(columna, "");
    }
  }
}
