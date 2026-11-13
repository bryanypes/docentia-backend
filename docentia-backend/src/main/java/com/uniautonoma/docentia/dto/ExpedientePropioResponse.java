package com.uniautonoma.docentia.dto;

import java.util.List;

/**
 * Lo que un docente ve de su propia evaluación. A propósito no tiene ningún campo con las notas de
 * cada fuente: como el tipo no las contiene, no hay forma de que salgan en la respuesta.
 *
 * @param periodo periodo activo, o nulo si no hay ninguno
 * @param promedio promedio ponderado del periodo activo, o nulo si no hay periodo
 * @param dimensiones resultado ya ponderado de cada dimensión en el periodo activo
 * @param resultadoHabilitado falso mientras la coordinación no registre la asistencia a la cita:
 *     entonces promedio y dimensiones llegan sin valor
 * @param clasesEnPeriodo clases (materias y grupos) del docente en el periodo activo; cero si no
 *     hay periodo. Con cero y sin ninguna nota, el docente no tiene materias que evaluar
 */
public record ExpedientePropioResponse(
    DocenteResponse docente,
    PeriodoResumenResponse periodo,
    PromedioPonderadoResponse promedio,
    List<ResultadoDimensionResponse> dimensiones,
    List<CompromisoResponse> compromisosVigentes,
    boolean resultadoHabilitado,
    int clasesEnPeriodo) {}
