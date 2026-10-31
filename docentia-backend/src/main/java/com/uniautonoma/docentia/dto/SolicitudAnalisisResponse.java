package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.CitaHallazgo;
import com.uniautonoma.docentia.entity.EstadoSolicitudAnalisis;
import com.uniautonoma.docentia.entity.Hallazgo;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Estado de una solicitud de análisis. El borrador solo viene cuando el estado es COMPLETADO.
 *
 * @param mensaje explicación para el usuario en ERROR o INSUFICIENTE
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
    Borrador borrador) {

  public static SolicitudAnalisisResponse desde(SolicitudAnalisis solicitud) {
    return desde(solicitud, Map.of());
  }

  /**
   * @param comentariosEnviados texto anonimizado de cada comentario que se envió al modelo, por id;
   *     con él cada cita muestra el comentario de origen
   */
  public static SolicitudAnalisisResponse desde(
      SolicitudAnalisis solicitud, Map<Long, String> comentariosEnviados) {
    Borrador borrador =
        solicitud.getEstado() == EstadoSolicitudAnalisis.COMPLETADO
            ? new Borrador(
                solicitud.getModelo(),
                solicitud.getVersionInstruccion(),
                solicitud.getFechaBorrador(),
                solicitud.getEstadoBorrador(),
                solicitud.getHallazgos().stream()
                    .map(hallazgo -> HallazgoBorrador.desde(hallazgo, comentariosEnviados))
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
        borrador);
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

  public record HallazgoBorrador(
      Long idHallazgo, String tipo, String texto, String estado, List<CitaBorrador> citas) {

    public static HallazgoBorrador desde(Hallazgo hallazgo) {
      return desde(hallazgo, Map.of());
    }

    static HallazgoBorrador desde(Hallazgo hallazgo, Map<Long, String> comentariosEnviados) {
      return new HallazgoBorrador(
          hallazgo.getIdHallazgo(),
          hallazgo.getTipo().name(),
          hallazgo.getTexto(),
          hallazgo.getEstado().name(),
          hallazgo.getCitas().stream()
              .map(cita -> CitaBorrador.desde(cita, comentariosEnviados))
              .toList());
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
