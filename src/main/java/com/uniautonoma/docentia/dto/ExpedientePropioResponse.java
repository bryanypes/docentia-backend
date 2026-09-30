package com.uniautonoma.docentia.dto;

/**
 * Lo que un docente ve de su propia evaluación. A propósito no tiene ningún campo con las notas de
 * coordinación o vicerrectoría: como el tipo no las contiene, no hay forma de que salgan en la
 * respuesta.
 *
 * @param periodo periodo activo, o nulo si no hay ninguno
 * @param promedio promedio ponderado del periodo activo, o nulo si no hay periodo
 */
public record ExpedientePropioResponse(
    DocenteResponse docente, PeriodoResumenResponse periodo, PromedioPonderadoResponse promedio) {}
