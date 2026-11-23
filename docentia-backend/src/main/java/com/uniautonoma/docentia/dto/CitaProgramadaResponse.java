package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.AsistenciaCita;
import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.ConfirmacionCita;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Una cita agendada, como la ve la administración: quién se reúne con quién y cuándo. Sin notas.
 *
 * @param rolAnfitrion COORDINADOR (con un docente) o VICERRECTOR (con un coordinador)
 * @param unidad programa del docente, o facultad si es un coordinador
 * @param confirmacion nula mientras el docente no responde el aviso
 * @param asistencia nula mientras no se registra la reunión
 */
public record CitaProgramadaResponse(
    Long idCita,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin,
    Long idDocente,
    String nombreDocente,
    Rol rolDocente,
    String unidad,
    String nombreAnfitrion,
    Rol rolAnfitrion,
    ConfirmacionCita confirmacion,
    AsistenciaCita asistencia) {

  public static CitaProgramadaResponse desde(Cita cita) {
    Usuario docente = cita.getDocente();
    Usuario anfitrion = cita.getFranja().getCoordinador();
    String unidad =
        docente.getPrograma() != null
            ? docente.getPrograma().getNombre()
            : docente.getFacultadDeAdscripcion() == null
                ? null
                : docente.getFacultadDeAdscripcion().getNombre();
    return new CitaProgramadaResponse(
        cita.getIdCita(),
        cita.getFranja().getFecha(),
        cita.getFranja().getHoraInicio(),
        cita.getFranja().getHoraFin(),
        docente.getIdUsuario(),
        docente.getNombreCompleto(),
        docente.getRol(),
        unidad,
        anfitrion.getNombreCompleto(),
        anfitrion.getRol(),
        cita.getConfirmacion(),
        cita.getAsistencia());
  }
}
