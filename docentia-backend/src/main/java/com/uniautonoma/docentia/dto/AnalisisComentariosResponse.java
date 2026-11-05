package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.FuenteComentarios;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis;
import java.time.LocalDateTime;

/**
 * Análisis de comentarios de un docente en un periodo.
 *
 * @param estado COMPLETADO, EN_COLA, EN_PROCESO, INSUFICIENTE, ERROR, SIN_COMENTARIOS o
 *     NO_DISPONIBLE_AUN
 * @param motivo detalle del estado: POCAS_ENCUESTAS, SIN_COMENTARIOS, TODOS_EXCLUIDOS,
 *     VENTANA_ABIERTA o SIN_SOLICITAR; nulo si no aplica
 * @param mensaje texto para mostrar al usuario
 * @param idSolicitud última solicitud, de la que sale el estado
 * @param version versión del resultado mostrado (la vigente), nula si no hay
 * @param estadoHallazgos PENDIENTE_VALIDACION o VALIDADO, de la solicitud del resultado vigente
 * @param resultado resultado vigente; puede venir aunque haya otra corrida en curso
 */
public record AnalisisComentariosResponse(
    String estado,
    String motivo,
    String mensaje,
    Long docenteId,
    String nombreDocente,
    Long periodoId,
    String nombrePeriodo,
    Referencia programa,
    Referencia facultad,
    FuenteComentarios fuente,
    Long idSolicitud,
    Integer version,
    LocalDateTime generadoEn,
    String estadoHallazgos,
    ResultadoAnalisis resultado) {

  /** Programa o facultad. */
  public record Referencia(Long id, String nombre) {}
}
