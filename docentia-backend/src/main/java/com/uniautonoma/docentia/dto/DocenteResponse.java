package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;

/**
 * Docente evaluado. Un coordinador también se evalúa como docente: no tiene programa, su facultad
 * es la que coordina y {@code esCoordinador} lo distingue (los estudiantes nunca lo ven).
 */
public record DocenteResponse(
    Long idDocente,
    String nombreCompleto,
    String correo,
    Long idPrograma,
    String nombrePrograma,
    Long idFacultad,
    String nombreFacultad,
    boolean esCoordinador) {

  public static DocenteResponse desde(Usuario docente) {
    Programa programa = docente.getPrograma();
    Facultad facultad = docente.getFacultadDeAdscripcion();
    return new DocenteResponse(
        docente.getIdUsuario(),
        docente.getNombreCompleto(),
        docente.getCorreo(),
        programa == null ? null : programa.getIdPrograma(),
        programa == null ? null : programa.getNombre(),
        facultad.getIdFacultad(),
        facultad.getNombre(),
        docente.getRol() == Rol.COORDINADOR);
  }
}
