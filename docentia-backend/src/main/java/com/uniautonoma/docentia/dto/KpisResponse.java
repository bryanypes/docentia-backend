package com.uniautonoma.docentia.dto;

import java.math.BigDecimal;

/**
 * Indicadores generales de un periodo dentro del alcance de quien consulta.
 *
 * @param periodoAnterior periodo contra el que se compara, o nulo si no existe
 * @param idFacultad facultad a la que se limitan los datos, o nulo si son institucionales
 * @param umbralDestacado promedio mínimo de un docente destacado
 */
public record KpisResponse(
    PeriodoResumenResponse periodo,
    PeriodoResumenResponse periodoAnterior,
    Long idFacultad,
    BigDecimal umbralDestacado,
    IndicadorResponse docentesEvaluados,
    IndicadorResponse promedioGeneral,
    IndicadorResponse evaluacionesRealizadas,
    IndicadorResponse docentesDestacados) {}
