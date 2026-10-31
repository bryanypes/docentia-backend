package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.config.GoogleProperties;
import com.uniautonoma.docentia.dto.CambioContrasenaRequest;
import com.uniautonoma.docentia.dto.InicioSesionGoogleRequest;
import com.uniautonoma.docentia.dto.InicioSesionRequest;
import com.uniautonoma.docentia.dto.SesionResponse;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.CredencialesInvalidasException;
import com.uniautonoma.docentia.exception.DatosInvalidosException;
import com.uniautonoma.docentia.exception.ServicioDeshabilitadoException;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.ProveedorTokenJwt;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.security.VerificadorTokenGoogle;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicio de sesión con correo y contraseña. Es el acceso temporal de desarrollo mientras se integra
 * el SSO institucional.
 */
@Service
public class AutenticacionService {

  private static final String SUFIJO_CORREO_INSTITUCIONAL = "@uniautonoma.edu.co";

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final ProveedorTokenJwt proveedorTokenJwt;
  private final VerificadorTokenGoogle verificadorTokenGoogle;
  private final GoogleProperties googleProperties;

  public AutenticacionService(
      UsuarioRepository usuarioRepository,
      PasswordEncoder passwordEncoder,
      ProveedorTokenJwt proveedorTokenJwt,
      VerificadorTokenGoogle verificadorTokenGoogle,
      GoogleProperties googleProperties) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.proveedorTokenJwt = proveedorTokenJwt;
    this.verificadorTokenGoogle = verificadorTokenGoogle;
    this.googleProperties = googleProperties;
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
   * Abre sesión con un ID token de Google. Solo acepta correos {@code @uniautonoma.edu.co} ya
   * registrados y activos en DOCENTIA.
   *
   * @param solicitud token emitido por Google Identity Services
   * @return token JWT y datos del usuario con su alcance
   * @throws ServicioDeshabilitadoException si no hay Client ID configurado
   * @throws CredencialesInvalidasException si el token, el dominio o la cuenta no son válidos
   */
  @Transactional(readOnly = true)
  public SesionResponse iniciarSesionConGoogle(InicioSesionGoogleRequest solicitud) {
    if (!googleProperties.habilitado()) {
      throw new ServicioDeshabilitadoException(
          "El inicio de sesión con Google no está configurado en este ambiente");
    }
    String correo = verificadorTokenGoogle.validarYExtraerCorreo(solicitud.idToken().trim());
    if (!esCorreoInstitucional(correo)) {
      throw new CredencialesInvalidasException();
    }
    Usuario usuario =
        usuarioRepository
            .findByCorreoIgnoreCaseAndEsActivoTrue(correo)
            .orElseThrow(CredencialesInvalidasException::new);
    return abrirSesion(usuario);
  }

  static boolean esCorreoInstitucional(String correo) {
    return correo != null && correo.toLowerCase(Locale.ROOT).endsWith(SUFIJO_CORREO_INSTITUCIONAL);
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
