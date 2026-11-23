package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.TipoFormulario;
import java.time.LocalDateTime;

/**
 * Formulario de un docente en el periodo activo, como lo ve la administración para reabrirlo. No
 * trae puntajes ni comentarios.
 *
 * @param nombreEvaluador quien lo responde: el propio docente (autoevaluación) o quien lo coevalúa
 * @param reabrible si se puede reabrir ahora
 * @param motivoNoReabrible por qué no, cuando no se puede
 */
public record FormularioEnviadoResponse(
    Long idFormulario,
    TipoFormulario tipo,
    String nombreEvaluador,
    String estado,
    LocalDateTime enviadaEn,
    boolean reabrible,
    String motivoNoReabrible) {}
