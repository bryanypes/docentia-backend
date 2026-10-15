package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.CitaHallazgo;
import com.uniautonoma.docentia.entity.EstadoSolicitudAnalisis;
import com.uniautonoma.docentia.entity.Hallazgo;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import java.time.LocalDateTime;
import java.util.List;

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
    Borrador borrador =
        solicitud.getEstado() == EstadoSolicitudAnalisis.COMPLETADO
            ? new Borrador(
                solicitud.getModelo(),
                solicitud.getVersionInstruccion(),
                solicitud.getFechaBorrador(),
                solicitud.getEstadoBorrador(),
                solicitud.getHallazgos().stream().map(HallazgoBorrador::desde).toList())
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

    static HallazgoBorrador desde(Hallazgo hallazgo) {
      return new HallazgoBorrador(
          hallazgo.getIdHallazgo(),
          hallazgo.getTipo().name(),
          hallazgo.getTexto(),
          hallazgo.getEstado().name(),
          hallazgo.getCitas().stream().map(CitaBorrador::desde).toList());
    }
  }

  public record CitaBorrador(Long idCita, Long idComentario, String fragmento) {

    static CitaBorrador desde(CitaHallazgo cita) {
      return new CitaBorrador(cita.getIdCita(), cita.getIdComentarioCitado(), cita.getFragmento());
    }
  }
}
