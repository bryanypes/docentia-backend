package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.CambioContrasenaRequest;
import com.uniautonoma.docentia.dto.InicioSesionGoogleRequest;
import com.uniautonoma.docentia.dto.InicioSesionRequest;
import com.uniautonoma.docentia.dto.PerfilResponse;
import com.uniautonoma.docentia.dto.SesionResponse;
import com.uniautonoma.docentia.entity.AccionAuditoria;
import com.uniautonoma.docentia.exception.CredencialesInvalidasException;
import com.uniautonoma.docentia.security.DireccionCliente;
import com.uniautonoma.docentia.security.LimitadorIntentos;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.AuditoriaService;
import com.uniautonoma.docentia.service.AutenticacionService;
import com.uniautonoma.docentia.service.PerfilService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de inicio de sesión. */
@RestController
@RequestMapping("/sesiones")
public class SesionController {

  private final AutenticacionService autenticacionService;
  private final LimitadorIntentos limitador;
  private final AuditoriaService auditoria;
  private final PerfilService perfilService;

  public SesionController(
      AutenticacionService autenticacionService,
      LimitadorIntentos limitador,
      AuditoriaService auditoria,
      PerfilService perfilService) {
    this.auditoria = auditoria;
    this.perfilService = perfilService;
    this.autenticacionService = autenticacionService;
    this.limitador = limitador;
  }

  /**
   * Abre una sesión con correo y contraseña.
   *
   * @param solicitud credenciales del usuario
   * @return token JWT y datos del usuario con su alcance
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SesionResponse iniciarSesion(
      @Valid @RequestBody InicioSesionRequest solicitud, HttpServletRequest peticion) {
    String ip = DireccionCliente.de(peticion);
    limitador.exigirPermitido(solicitud.correo(), ip);
    try {
      SesionResponse sesion = autenticacionService.iniciarSesion(solicitud);
      limitador.registrarExito(solicitud.correo());
      auditoria.registrar(
          AccionAuditoria.LOGIN,
          "USUARIO",
          null,
          null,
          solicitud.correo(),
          null,
          AuditoriaService.EXITO);
      return sesion;
    } catch (CredencialesInvalidasException rechazo) {
      int usados = limitador.registrarFallo(solicitud.correo(), ip);
      auditoria.registrar(
          AccionAuditoria.LOGIN,
          "USUARIO",
          null,
          null,
          solicitud.correo(),
          null,
          AuditoriaService.FALLO);
      throw rechazo.conIntentos(usados, limitador.getMaximoPorCorreo());
    }
  }

  /**
   * Abre una sesión con Google. El correo del token debe ser {@code @uniautonoma.edu.co} y existir
   * en DOCENTIA.
   *
   * @param solicitud ID token de Google
   * @return token JWT y datos del usuario con su alcance
   */
  @PostMapping("/google")
  @ResponseStatus(HttpStatus.CREATED)
  public SesionResponse iniciarSesionConGoogle(
      @Valid @RequestBody InicioSesionGoogleRequest solicitud) {
    return autenticacionService.iniciarSesionConGoogle(solicitud);
  }

  /**
   * Devuelve el usuario dueño del token enviado.
   *
   * @param sesion sesión leída del token
   * @return usuario autenticado con su rol y alcance
   */
  @GetMapping("/actual")
  public UsuarioSesion obtenerSesionActual(@AuthenticationPrincipal UsuarioSesion sesion) {
    return sesion;
  }

  /**
   * Perfil del usuario autenticado, con su facultad y programa por nombre.
   *
   * @param sesion sesión leída del token
   * @return datos propios de solo lectura
   */
  @GetMapping("/actual/perfil")
  public PerfilResponse obtenerPerfil(@AuthenticationPrincipal UsuarioSesion sesion) {
    return perfilService.obtener(sesion);
  }

  /**
   * Cambia la contraseña propia. Es lo único que acepta una sesión con la contraseña temporal.
   *
   * @param sesion sesión leída del token
   * @param solicitud contraseña actual y nueva
   * @return sesión nueva, sin la marca de contraseña temporal
   */
  @PutMapping("/actual/contrasena")
  public SesionResponse cambiarContrasena(
      @AuthenticationPrincipal UsuarioSesion sesion,
      @Valid @RequestBody CambioContrasenaRequest solicitud) {
    return autenticacionService.cambiarContrasena(sesion, solicitud);
  }
}
