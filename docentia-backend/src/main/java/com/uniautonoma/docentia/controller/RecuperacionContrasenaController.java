package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.security.DireccionCliente;
import com.uniautonoma.docentia.service.RecuperacionContrasenaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Recuperación de contraseña sin sesión: solo estas tres rutas son públicas. */
@RestController
@RequestMapping("/sesiones/recuperacion")
public class RecuperacionContrasenaController {

  private final RecuperacionContrasenaService servicio;
  private final boolean hayCorreo;

  public RecuperacionContrasenaController(
      RecuperacionContrasenaService servicio,
      com.uniautonoma.docentia.config.NotificacionProperties notificaciones) {
    this.servicio = servicio;
    this.hayCorreo = notificaciones.correo().configurado();
  }

  /**
   * Si el servidor puede enviar el enlace por correo. Sin SMTP la pantalla no promete un correo y
   * dice a quién pedir una contraseña nueva. Solo dice sí o no; no expone la configuración.
   */
  @GetMapping("/disponibilidad")
  public Map<String, Boolean> disponibilidad() {
    return Map.of("porCorreo", hayCorreo);
  }

  /** Siempre responde lo mismo, exista o no el correo. */
  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, String> solicitar(
      @Valid @RequestBody SolicitudRecuperacion solicitud, HttpServletRequest peticion) {
    servicio.solicitar(solicitud.correo(), DireccionCliente.de(peticion));
    return Map.of("mensaje", RecuperacionContrasenaService.MENSAJE_GENERICO);
  }

  @PostMapping("/restablecimiento")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void restablecer(
      @Valid @RequestBody Restablecimiento solicitud, HttpServletRequest peticion) {
    servicio.restablecer(solicitud.token(), solicitud.contrasena(), DireccionCliente.de(peticion));
  }

  public record SolicitudRecuperacion(
      @NotBlank(message = "Escribe tu correo")
          @Email(message = "El correo no tiene un formato válido")
          @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
          String correo) {}

  public record Restablecimiento(
      @NotBlank(message = "Falta el enlace de recuperación")
          @Size(max = 100, message = "El enlace no es válido")
          String token,
      @NotBlank(message = "Escribe la contraseña nueva")
          @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
          String contrasena) {}
}
