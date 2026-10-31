package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Importacion;
import com.uniautonoma.docentia.entity.ResultadoImportacion;
import java.time.LocalDateTime;

/** Una carga de CSV del historial. */
public record ImportacionResponse(
    Long idImportacion,
    String nombreArchivo,
    String hashSha256,
    Long idPeriodo,
    String nombrePeriodo,
    Long idUsuario,
    String nombreUsuario,
    LocalDateTime fechaImportacion,
    ResultadoImportacion resultado,
    String codificacion,
    String delimitador,
    int filasProcesadas,
    int filasConError,
    int docentesAfectados,
    int notasNuevas,
    int notasCorregidas,
    int notasSinCambios,
    int comentariosNuevos,
    int comentariosRepetidos) {

  public static ImportacionResponse desde(Importacion importacion) {
    return new ImportacionResponse(
        importacion.getIdImportacion(),
        importacion.getNombreArchivo(),
        importacion.getHashSha256(),
        importacion.getPeriodo().getIdPeriodo(),
        importacion.getPeriodo().getNombre(),
        importacion.getUsuario().getIdUsuario(),
        importacion.getUsuario().getNombreCompleto(),
        importacion.getFechaImportacion(),
        importacion.getResultado(),
        importacion.getCodificacion(),
        importacion.getDelimitador(),
        importacion.getFilasProcesadas(),
        importacion.getFilasConError(),
        importacion.getDocentesAfectados(),
        importacion.getNotasNuevas(),
        importacion.getNotasCorregidas(),
        importacion.getNotasSinCambios(),
        importacion.getComentariosNuevos(),
        importacion.getComentariosRepetidos());
  }
}
