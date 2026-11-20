package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.CitaHallazgo;
import com.uniautonoma.docentia.entity.EstadoSolicitudAnalisis;
import com.uniautonoma.docentia.entity.FuenteComentarios;
import com.uniautonoma.docentia.entity.Hallazgo;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import com.uniautonoma.docentia.service.analisis.CriteriosEvaluacion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Estado de una solicitud de análisis. El borrador solo viene cuando el estado es COMPLETADO.
 *
 * @param mensaje explicación para el usuario en ERROR o INSUFICIENTE
 * @param fuente CSV o FORMULARIO
 * @param motivo detalle de INSUFICIENTE: POCAS_ENCUESTAS, SIN_COMENTARIOS, TODOS_EXCLUIDOS o
 *     VENTANA_ABIERTA
 */
public record SolicitudAnalisisResponse(
    Long idSolicitud,
    Long idDocente,
    String nombreDocente,
    Long idPeriodo,
    String nombrePeriodo,
    EstadoSolicitudAnalisis estado,
    String mensaje,
    Integer encuestasCerradas,
    Integer comentariosEnviados,
    Integer comentariosFiltrados,
    LocalDateTime fechaSolicitud,
    LocalDateTime fechaInicio,
    LocalDateTime fechaFin,
    Borrador borrador,
    FuenteComentarios fuente,
    String motivo,
    Integer comentariosNuevos,
    Integer comentariosRecibidos,
    LocalDateTime nuevoAnalisisDesde) {

  public static SolicitudAnalisisResponse desde(SolicitudAnalisis solicitud) {
    return desde(solicitud, Map.of(), Map.of());
  }

  /**
   * Con los comentarios que llegaron después del análisis: si es mayor que 0 la coordinación ve
   * "tenemos nuevos comentarios de estudiantes, por favor actualiza el análisis de IA".
   */
  public SolicitudAnalisisResponse conComentariosNuevos(Integer nuevos) {
    return new SolicitudAnalisisResponse(
        idSolicitud,
        idDocente,
        nombreDocente,
        idPeriodo,
        nombrePeriodo,
        estado,
        mensaje,
        encuestasCerradas,
        comentariosEnviados,
        comentariosFiltrados,
        fechaSolicitud,
        fechaInicio,
        fechaFin,
        borrador,
        fuente,
        motivo,
        nuevos,
        comentariosRecibidos,
        nuevoAnalisisDesde);
  }

  /** Desde cuándo se puede volver a analizar sin que hayan llegado comentarios nuevos. */
  public SolicitudAnalisisResponse conNuevoAnalisisDesde(LocalDateTime desde) {
    return new SolicitudAnalisisResponse(
        idSolicitud,
        idDocente,
        nombreDocente,
        idPeriodo,
        nombrePeriodo,
        estado,
        mensaje,
        encuestasCerradas,
        comentariosEnviados,
        comentariosFiltrados,
        fechaSolicitud,
        fechaInicio,
        fechaFin,
        borrador,
        fuente,
        motivo,
        comentariosNuevos,
        comentariosRecibidos,
        desde);
  }

  /**
   * @param comentariosEnviados texto anonimizado de cada comentario que se envió al modelo, por id;
   *     con él cada cita muestra el comentario de origen
   */
  /**
   * @param cambios por hallazgo, cuántas veces lo cambió una persona; sin cambios no hay historial
   */
  public static SolicitudAnalisisResponse desde(
      SolicitudAnalisis solicitud,
      Map<Long, String> comentariosEnviados,
      Map<Long, Integer> cambios) {
    Borrador borrador =
        solicitud.getEstado() == EstadoSolicitudAnalisis.COMPLETADO
            ? new Borrador(
                solicitud.getModelo(),
                solicitud.getVersionInstruccion(),
                solicitud.getFechaBorrador(),
                solicitud.getEstadoBorrador(),
                solicitud.getHallazgos().stream()
                    .map(
                        hallazgo ->
                            HallazgoBorrador.desde(
                                hallazgo,
                                comentariosEnviados,
                                cambios.getOrDefault(hallazgo.getIdHallazgo(), 0)))
                    .toList())
            : null;
    return new SolicitudAnalisisResponse(
        solicitud.getIdSolicitud(),
        solicitud.getDocente().getIdUsuario(),
        solicitud.getDocente().getNombreCompleto(),
        solicitud.getPeriodo().getIdPeriodo(),
        solicitud.getPeriodo().getNombre(),
        solicitud.getEstado(),
        solicitud.getMensaje(),
        solicitud.getEncuestasCerradas(),
        solicitud.getComentariosEnviados(),
        solicitud.getComentariosFiltrados(),
        solicitud.getFechaSolicitud(),
        solicitud.getFechaInicio(),
        solicitud.getFechaFin(),
        borrador,
        solicitud.getFuente(),
        solicitud.getMotivo(),
        null,
        solicitud.getComentariosLeidos(),
        null);
  }

  /**
   * Borrador generado por el modelo.
   *
   * @param versionInstruccion versión del texto de instrucción (resources/prompts)
   * @param estado PENDIENTE_VALIDACION hasta que coordinación lo valide
   */
  public record Borrador(
      String modelo,
      String versionInstruccion,
      LocalDateTime fechaGeneracion,
      String estado,
      List<HallazgoBorrador> hallazgos) {}

  /**
   * @param criterio criterio de la evaluación (C1 a C6) que agrupa sus comentarios; nulo en
   *     análisis anteriores
   * @param cambios cuántas veces lo incluyó, descartó o editó una persona
   */
  public record HallazgoBorrador(
      Long idHallazgo,
      String tipo,
      String texto,
      String estado,
      List<CitaBorrador> citas,
      String criterio,
      String nombreCriterio,
      int cambios) {

    public static HallazgoBorrador desde(Hallazgo hallazgo) {
      return desde(hallazgo, Map.of(), 0);
    }

    static HallazgoBorrador desde(
        Hallazgo hallazgo, Map<Long, String> comentariosEnviados, int cambios) {
      return new HallazgoBorrador(
          hallazgo.getIdHallazgo(),
          hallazgo.getTipo().name(),
          hallazgo.getTexto(),
          hallazgo.getEstado().name(),
          hallazgo.getCitas().stream()
              .map(cita -> CitaBorrador.desde(cita, comentariosEnviados))
              .toList(),
          hallazgo.getCriterio(),
          CriteriosEvaluacion.nombre(hallazgo.getCriterio()),
          cambios);
    }
  }

  /**
   * @param verificada el fragmento aparece en un comentario no filtrado del docente y periodo
   * @param comentario texto anonimizado del comentario citado, tal como lo recibió el modelo; nulo
   *     si el id no es de un comentario enviado (cita inventada o de un comentario filtrado)
   */
  public record CitaBorrador(
      Long idCita, Long idComentario, String fragmento, boolean verificada, String comentario) {

    static CitaBorrador desde(CitaHallazgo cita, Map<Long, String> comentariosEnviados) {
      Long idComentario = cita.getIdComentarioCitado();
      return new CitaBorrador(
          cita.getIdCita(),
          idComentario,
          cita.getFragmento(),
          cita.isVerificada(),
          idComentario == null ? null : comentariosEnviados.get(idComentario));
    }
  }
}
