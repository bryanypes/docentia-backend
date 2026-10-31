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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Autoevaluación del docente autenticado en el periodo activo (20 % de la nota). */
@RestController
@RequestMapping("/autoevaluacion")
@PreAuthorize("hasRole('DOCENTE')")
public class AutoevaluacionController {

  private final CalificacionService calificacionService;

  public AutoevaluacionController(CalificacionService calificacionService) {
    this.calificacionService = calificacionService;
  }

  /**
   * Consulta la autoevaluación propia.
   *
   * @param sesion docente autenticado
   * @return la autoevaluación, o 204 si todavía no existe
   */
  @GetMapping
  public ResponseEntity<CalificacionResponse> obtener(
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return calificacionService
        .obtenerAutoevaluacion(sesion)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  /**
   * Registra o corrige la autoevaluación propia.
   *
   * @param solicitud notas de los cinco aspectos y observación opcional
   * @param sesion docente autenticado
   * @return la autoevaluación guardada
   */
  @PutMapping
  public CalificacionResponse registrar(
      @Valid @RequestBody CalificacionRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return calificacionService.registrarAutoevaluacion(solicitud, sesion);
  }
}
