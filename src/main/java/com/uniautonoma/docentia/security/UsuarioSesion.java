package com.uniautonoma.docentia.security;

import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Datos del usuario que viajan en la sesión. El alcance (facultad y programa) se resuelve una sola
 * vez al iniciar sesión para no volver a consultarlo en cada petición.
 */
public record UsuarioSesion(
    Long idUsuario,
    String nombreCompleto,
    String correo,
    Rol rol,
    Long idFacultad,
    Long idPrograma) {

  public static UsuarioSesion desde(Usuario usuario) {
    Programa programa = usuario.getPrograma();
    if (programa != null) {
      return crear(usuario, programa.getFacultad().getIdFacultad(), programa.getIdPrograma());
    }
    Long idFacultad = usuario.getFacultad() != null ? usuario.getFacultad().getIdFacultad() : null;
    return crear(usuario, idFacultad, null);
  }

  private static UsuarioSesion crear(Usuario usuario, Long idFacultad, Long idPrograma) {
    return new UsuarioSesion(
        usuario.getIdUsuario(),
        usuario.getNombreCompleto(),
        usuario.getCorreo(),
        usuario.getRol(),
        idFacultad,
        idPrograma);
  }

  public Authentication aAutenticacion() {
    return new UsernamePasswordAuthenticationToken(
        this, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol.name())));
  }

  public boolean esCoordinador() {
    return rol == Rol.COORDINADOR;
  }

  public boolean esVicerrector() {
    return rol == Rol.VICERRECTOR;
  }

  public boolean esDocente() {
    return rol == Rol.DOCENTE;
  }
}
