package com.uniautonoma.docentia.security;

import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Datos del usuario que viajan en la sesión. El alcance (facultad y programa) se resuelve una sola
 * vez al iniciar sesión para no volver a consultarlo en cada petición.
 *
 * @param debeCambiarContrasena la contraseña es temporal: hasta cambiarla la sesión no sirve para
 *     nada más que consultarse a sí misma y cambiar la contraseña
 */
public record UsuarioSesion(
    Long idUsuario,
    String nombreCompleto,
    String correo,
    Rol rol,
    Long idFacultad,
    Long idPrograma,
    boolean debeCambiarContrasena) {

  /** Autoridad que reemplaza al rol mientras la contraseña sea temporal. */
  public static final String AUTORIDAD_CONTRASENA_TEMPORAL = "CONTRASENA_TEMPORAL";

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
        idPrograma,
        usuario.getDebeCambiarContrasena());
  }

  /**
   * Con la contraseña temporal la sesión no lleva el rol: ningún endpoint protegido por rol la
   * acepta, y las reglas generales de SecurityConfig dejan pasar solo el cambio de contraseña.
   */
  public Authentication aAutenticacion() {
    GrantedAuthority autoridad =
        new SimpleGrantedAuthority(
            debeCambiarContrasena ? AUTORIDAD_CONTRASENA_TEMPORAL : "ROLE_" + rol.name());
    return new UsernamePasswordAuthenticationToken(this, null, List.of(autoridad));
  }

  /**
   * El vicerrector alcanza todas las facultades y el coordinador solo la suya. El docente no
   * consulta información de facultades, así que para él siempre es falso.
   */
  public boolean tieneAlcanceSobreFacultad(Long idFacultadConsultada) {
    if (esVicerrector()) {
      return true;
    }
    return esCoordinador() && idFacultad.equals(idFacultadConsultada);
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
