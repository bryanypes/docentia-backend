package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Usuario;

public record DocenteResponse(
    Long idDocente,
    String nombreCompleto,
    String correo,
    Long idPrograma,
    String nombrePrograma,
    Long idFacultad,
    String nombreFacultad) {

  public static DocenteResponse desde(Usuario docente) {
    Programa programa = docente.getPrograma();
    return new DocenteResponse(
        docente.getIdUsuario(),
        docente.getNombreCompleto(),
        docente.getCorreo(),
        programa.getIdPrograma(),
        programa.getNombre(),
        programa.getFacultad().getIdFacultad(),
        programa.getFacultad().getNombre());
  }
}
