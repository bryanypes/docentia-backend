package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.service.importacion.ValidadorEvaluacionEstudiantes.ErrorFila;

/**
 * Un problema del archivo. fila y columna son nulas si el problema es del archivo completo (por
 * ejemplo, una columna obligatoria sin asignar); columna es nula si es de la fila completa.
 */
public record ErrorFilaResponse(Integer fila, String columna, String valor, String motivo) {

  public static ErrorFilaResponse desde(ErrorFila error) {
    return new ErrorFilaResponse(error.fila(), error.columna(), error.valor(), error.motivo());
  }

  public static ErrorFilaResponse delArchivo(String motivo) {
    return new ErrorFilaResponse(null, null, null, motivo);
  }
}
