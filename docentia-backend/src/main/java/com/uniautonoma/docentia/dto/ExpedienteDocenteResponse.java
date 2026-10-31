package com.uniautonoma.docentia.dto;

import java.util.List;

/**
 * Expediente completo de un docente, para coordinación y vicerrectoría.
 *
 * @param periodo periodo activo, o nulo si no hay ninguno
 * @param promedio promedio ponderado del periodo activo, o nulo si no hay periodo
 * @param notasPorFuente una entrada por fuente, con nota nula en las que no han calificado
 * @param historico promedios de los periodos anteriores en los que el docente fue calificado
 */
public record ExpedienteDocenteResponse(
    DocenteResponse docente,
    PeriodoResumenResponse periodo,
    PromedioPonderadoResponse promedio,
    List<NotaFuenteResponse> notasPorFuente,
    List<DimensionPorFuenteResponse> dimensiones,
    List<PromedioHistoricoResponse> historico,
    List<CompromisoResponse> compromisosVigentes) {}
