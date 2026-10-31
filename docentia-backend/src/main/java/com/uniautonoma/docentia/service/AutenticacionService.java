package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.CambioContrasenaRequest;
import com.uniautonoma.docentia.dto.InicioSesionRequest;
import com.uniautonoma.docentia.dto.SesionResponse;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.CredencialesInvalidasException;
import com.uniautonoma.docentia.exception.DatosInvalidosException;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.ProveedorTokenJwt;
import com.uniautonoma.docentia.security.UsuarioSesion;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicio de sesión con correo y contraseña. Es el acceso temporal de desarrollo mientras se integra
 * el SSO institucional.
 */
@Service
public class AutenticacionService {

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final ProveedorTokenJwt proveedorTokenJwt;

  public AutenticacionService(
      UsuarioRepository usuarioRepository,
      PasswordEncoder passwordEncoder,
      ProveedorTokenJwt proveedorTokenJwt) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.proveedorTokenJwt = proveedorTokenJwt;
  }

  /**
   * Valida las credenciales y abre una sesión con el alcance del usuario.
   *
   * @param solicitud correo y contraseña
   * @return token de sesión junto con el rol y el alcance del usuario
   * @throws CredencialesInvalidasException si el correo no existe, el usuario está inactivo o la
   *     contraseña no coincide; el mensaje es el mismo en los tres casos
   */
  @Transactional(readOnly = true)
  public SesionResponse iniciarSesion(InicioSesionRequest solicitud) {
    Usuario usuario =
        usuarioRepository
            .findByCorreoIgnoreCaseAndEsActivoTrue(solicitud.correo().trim())
            .filter(
                encontrado ->
                    passwordEncoder.matches(solicitud.contrasena(), encontrado.getContrasenaHash()))
            .orElseThrow(CredencialesInvalidasException::new);
    return abrirSesion(usuario);
  }

  /**
   * Cambia la contraseña del dueño de la sesión y le abre una sesión nueva, ya sin la marca de
   * contraseña temporal.
   *
   * @param sesion sesión actual, aunque tenga la contraseña temporal
   * @param solicitud contraseña actual y nueva
   * @return token nuevo y datos del usuario
   * @throws CredencialesInvalidasException si la cuenta ya no existe o está inactiva
   * @throws DatosInvalidosException si la actual no coincide o la nueva es igual a la actual
   */
  @Transactional
  public SesionResponse cambiarContrasena(UsuarioSesion sesion, CambioContrasenaRequest solicitud) {
    Usuario usuario =
        usuarioRepository
            .findById(sesion.idUsuario())
            .filter(Usuario::getEsActivo)
            .orElseThrow(CredencialesInvalidasException::new);
    if (!passwordEncoder.matches(solicitud.contrasenaActual(), usuario.getContrasenaHash())) {
      throw new DatosInvalidosException("La contraseña actual no es correcta", "contrasenaActual");
    }
    if (solicitud.contrasenaNueva().equals(solicitud.contrasenaActual())) {
      throw new DatosInvalidosException(
          "La contraseña nueva debe ser distinta de la actual", "contrasenaNueva");
    }
    usuario.cambiarContrasenaPropia(passwordEncoder.encode(solicitud.contrasenaNueva()));
    return abrirSesion(usuarioRepository.saveAndFlush(usuario));
  }

  private SesionResponse abrirSesion(Usuario usuario) {
    UsuarioSesion sesion = UsuarioSesion.desde(usuario);
    return new SesionResponse(proveedorTokenJwt.generarToken(sesion), sesion);
  }
}
