package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.service.importacion.CampoImportacion;
import com.uniautonoma.docentia.service.importacion.ResolutorMapeo.OrigenMapeo;
import java.util.List;
import java.util.Map;

/**
 * Resultado de revisar un CSV, antes o en el momento de importarlo.
 *
 * @param valido verdadero solo si no hay ningún error; con un error no se importa nada
 * @param mapeo columna del archivo → campo asignado; las columnas que no aparecen se ignoran
 * @param origenMapeo MANUAL (lo envió el usuario), PLANTILLA o AUTOMATICO (por el nombre de la
 *     columna); nulo si el archivo no se pudo leer
 * @param plantilla nombre de la plantilla aplicada, o nulo
 * @param columnasFaltantes columnas de la plantilla que el archivo ya no trae
 * @param columnasNuevas columnas del archivo que la plantilla no conocía
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
    OrigenMapeo origenMapeo,
    String plantilla,
    List<String> columnasFaltantes,
    List<String> columnasNuevas,
    List<CampoImportacion> camposObligatoriosSinMapear,
    int filasProcesadas,
    int filasConError,
    List<ErrorFilaResponse> errores,
    List<DocenteImportacionResponse> docentes,
    List<String> advertencias) {}
