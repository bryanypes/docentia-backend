package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.ConteoNoLeidasResponse;
import com.uniautonoma.docentia.dto.NotificacionResponse;
import com.uniautonoma.docentia.dto.PaginaResponse;
import com.uniautonoma.docentia.dto.PreferenciasNotificacionRequest;
import com.uniautonoma.docentia.dto.PreferenciasNotificacionResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.notificacion.NotificacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Bandeja de avisos del usuario autenticado; cada operación actúa solo sobre lo propio. */
@RestController
@RequestMapping("/notificaciones")
public class NotificacionController {

  private final NotificacionService notificacionService;

  public NotificacionController(NotificacionService notificacionService) {
    this.notificacionService = notificacionService;
  }

  @GetMapping
  public PaginaResponse<NotificacionResponse> listar(
      @RequestParam(defaultValue = "false") boolean soloNoLeidas,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return notificacionService.listar(sesion.idUsuario(), soloNoLeidas, page, size);
  }

  @GetMapping("/no-leidas/conteo")
  public ConteoNoLeidasResponse contarNoLeidas(@AuthenticationPrincipal UsuarioSesion sesion) {
    return new ConteoNoLeidasResponse(notificacionService.contarNoLeidas(sesion.idUsuario()));
  }

  @PatchMapping("/{id}/leida")
  public NotificacionResponse marcarLeida(
      @PathVariable Long id, @AuthenticationPrincipal UsuarioSesion sesion) {
    return notificacionService.marcarLeida(id, sesion.idUsuario());
  }

  @PatchMapping("/leer-todas")
  public ResponseEntity<Void> leerTodas(@AuthenticationPrincipal UsuarioSesion sesion) {
    notificacionService.marcarTodasLeidas(sesion.idUsuario());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/preferencias")
  public PreferenciasNotificacionResponse preferencias(
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return notificacionService.leerPreferencias(sesion.idUsuario());
  }

  @PutMapping("/preferencias")
  public PreferenciasNotificacionResponse guardarPreferencias(
      @Valid @RequestBody PreferenciasNotificacionRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return notificacionService.guardarPreferencias(
        sesion.idUsuario(), solicitud.correoHabilitado());
  }
}
