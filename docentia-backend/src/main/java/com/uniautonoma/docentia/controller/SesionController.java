package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.CambioContrasenaRequest;
import com.uniautonoma.docentia.dto.InicioSesionRequest;
import com.uniautonoma.docentia.dto.SesionResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.AutenticacionService;
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

  public SesionController(AutenticacionService autenticacionService) {
    this.autenticacionService = autenticacionService;
  }

  /**
   * Abre una sesión con correo y contraseña.
   *
   * @param solicitud credenciales del usuario
   * @return token JWT y datos del usuario con su alcance
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SesionResponse iniciarSesion(@Valid @RequestBody InicioSesionRequest solicitud) {
    return autenticacionService.iniciarSesion(solicitud);
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
