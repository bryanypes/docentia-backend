package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.InicioSesionRequest;
import com.uniautonoma.docentia.dto.SesionResponse;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.CredencialesInvalidasException;
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
    UsuarioSesion sesion = UsuarioSesion.desde(usuario);
    return new SesionResponse(proveedorTokenJwt.generarToken(sesion), sesion);
  }
}
