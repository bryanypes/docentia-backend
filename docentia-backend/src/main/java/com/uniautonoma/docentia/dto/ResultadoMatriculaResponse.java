package com.uniautonoma.docentia.dto;

import java.util.List;

/** Resultado de importar la matrícula: todo o nada. */
public record ResultadoMatriculaResponse(
    boolean valido,
    int filasProcesadas,
    List<ErrorFila> errores,
    int estudiantesCreados,
    int matriculasNuevas,
    int matriculasRetiradas) {

  public record ErrorFila(int fila, String motivo) {}
}
