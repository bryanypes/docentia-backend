package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.EstadoSolicitudReapertura;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.SolicitudReapertura;
import com.uniautonoma.docentia.entity.TipoFormulario;
import com.uniautonoma.docentia.entity.Usuario;
import java.time.LocalDateTime;

/** Ticket de corrección de un formulario. Nunca lleva notas ni respuestas. */
public record SolicitudReaperturaResponse(
    Long idSolicitud,
    TipoFormulario tipo,
    EstadoSolicitudReapertura estado,
    String periodo,
    Long idDocente,
    String nombreDocente,
    String nombreFacultad,
    String nombreSolicitante,
    String motivo,
    Rol rolAprobador,
    String nombreAprobador,
    String respuestaAprobador,
    LocalDateTime fechaAprobacion,
    String nombreAdministrador,
    String respuestaAdministrador,
    LocalDateTime fechaAtencion,
    LocalDateTime fechaSolicitud) {

  public static SolicitudReaperturaResponse desde(SolicitudReapertura s) {
    Usuario docente = s.getFormulario().getDocente();
    return new SolicitudReaperturaResponse(
        s.getId(),
        s.getFormulario().getTipo(),
        s.getEstado(),
        s.getFormulario().getPeriodo().getNombre(),
        docente.getIdUsuario(),
        docente.getNombreCompleto(),
        docente.getFacultadDeAdscripcion() == null
            ? null
            : docente.getFacultadDeAdscripcion().getNombre(),
        s.getSolicitante().getNombreCompleto(),
        s.getMotivo(),
        s.getRolAprobador(),
        s.getAprobador() == null ? null : s.getAprobador().getNombreCompleto(),
        s.getRespuestaAprobador(),
        s.getFechaAprobacion(),
        s.getAdministrador() == null ? null : s.getAdministrador().getNombreCompleto(),
        s.getRespuestaAdministrador(),
        s.getFechaAtencion(),
        s.getFechaSolicitud());
  }
}
