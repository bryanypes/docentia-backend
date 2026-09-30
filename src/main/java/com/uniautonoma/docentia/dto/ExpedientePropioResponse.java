package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import java.util.List;

/**
 * Lo que un docente ve de su propia evaluación. A propósito no tiene ningún campo con las notas de
 * coordinación o vicerrectoría: como el tipo no las contiene, no hay forma de que salgan en la
 * respuesta.
 */
public record ExpedientePropioResponse(
    DocenteResponse docente,
    PeriodoResumenResponse periodo,
    List<FuenteEvaluacion> fuentesPendientes) {}
