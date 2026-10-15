package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;

/** Usuario con su rol y su alcance. La facultad del docente es la de su programa. */
public record UsuarioResponse(
    Long idUsuario,
    String nombreCompleto,
    String correo,
    Rol rol,
    Long idFacultad,
    String nombreFacultad,
    Long idPrograma,
    String nombrePrograma,
    boolean esActivo) {

  public static UsuarioResponse desde(Usuario usuario) {
    Programa programa = usuario.getPrograma();
    Facultad facultad = programa != null ? programa.getFacultad() : usuario.getFacultad();
    return new UsuarioResponse(
        usuario.getIdUsuario(),
        usuario.getNombreCompleto(),
        usuario.getCorreo(),
        usuario.getRol(),
        facultad != null ? facultad.getIdFacultad() : null,
        facultad != null ? facultad.getNombre() : null,
        programa != null ? programa.getIdPrograma() : null,
        programa != null ? programa.getNombre() : null,
        usuario.getEsActivo());
  }
}
