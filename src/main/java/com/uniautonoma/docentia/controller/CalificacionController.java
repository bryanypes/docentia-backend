package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.CalificacionRequest;
import com.uniautonoma.docentia.dto.CalificacionResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.CalificacionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de calificación de un docente en el periodo activo. */
@RestController
@RequestMapping("/docentes/{idDocente}/calificaciones")
public class CalificacionController {

  private final CalificacionService calificacionService;

  public CalificacionController(CalificacionService calificacionService) {
    this.calificacionService = calificacionService;
  }

  /**
   * Consulta la calificación que coordinación ya registró para el docente.
   *
   * @param idDocente identificador del docente
   * @param sesion coordinador autenticado
   * @return la calificación, o 204 si todavía no existe
   */
  @GetMapping("/coordinacion")
  @PreAuthorize("hasRole('COORDINADOR')")
  public ResponseEntity<CalificacionResponse> obtenerCalificacionCoordinacion(
      @PathVariable Long idDocente, @AuthenticationPrincipal UsuarioSesion sesion) {
    return calificacionService
        .obtenerCalificacion(idDocente, sesion)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  /**
   * Registra o reemplaza la calificación de coordinación del docente.
   *
   * @param idDocente identificador del docente
   * @param solicitud notas de los cinco aspectos y observación opcional
   * @param sesion coordinador autenticado
   * @return la calificación guardada
   */
  @PutMapping("/coordinacion")
  @PreAuthorize("hasRole('COORDINADOR')")
  public CalificacionResponse calificarComoCoordinacion(
      @PathVariable Long idDocente,
      @Valid @RequestBody CalificacionRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return calificacionService.calificarDocente(idDocente, solicitud, sesion);
  }
}
