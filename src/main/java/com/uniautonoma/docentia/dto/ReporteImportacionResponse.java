package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.service.importacion.CampoImportacion;
import java.util.List;
import java.util.Map;

/**
 * Resultado de revisar un CSV, antes o en el momento de importarlo.
 *
 * @param valido verdadero solo si no hay ningún error; con un error no se importa nada
 * @param mapeo columna del archivo → campo asignado; las columnas que no aparecen se ignoran
 * @param camposObligatoriosSinMapear campos que ninguna columna cubre
 * @param errores como máximo los primeros 500; filasConError tiene el total
 */
public record ReporteImportacionResponse(
    boolean valido,
    String nombreArchivo,
    String hashSha256,
    String codificacion,
    String delimitador,
    List<String> columnas,
    Map<String, CampoImportacion> mapeo,
    List<CampoImportacion> camposObligatoriosSinMapear,
    int filasProcesadas,
    int filasConError,
    List<ErrorFilaResponse> errores,
    List<DocenteImportacionResponse> docentes,
    List<String> advertencias) {}
