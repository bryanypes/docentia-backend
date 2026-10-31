package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.CitaResponse;
import com.uniautonoma.docentia.dto.FranjaDisponibleResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.CitaService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Reserva de franjas por el docente. */
@RestController
@PreAuthorize("hasRole('DOCENTE')")
public class CitaController {

  private final CitaService citaService;

  public CitaController(CitaService citaService) {
    this.citaService = citaService;
  }

  /** Franjas que el docente puede reservar con el coordinador de su facultad. */
  @GetMapping("/franjas/disponibles")
  public List<FranjaDisponibleResponse> listarDisponibles(
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return citaService.listarDisponibles(sesion);
  }

  /** Reserva una franja; si el docente ya tenía cita en el periodo, la libera. */
  @PostMapping("/franjas/{idFranja}/reserva")
  @ResponseStatus(HttpStatus.CREATED)
  public CitaResponse reservar(
      @PathVariable Long idFranja, @AuthenticationPrincipal UsuarioSesion sesion) {
    return citaService.reservar(idFranja, sesion);
  }

  /** Cita activa del docente en el periodo activo, o 204 si no tiene. */
  @GetMapping("/citas/mia")
  public ResponseEntity<CitaResponse> miCita(@AuthenticationPrincipal UsuarioSesion sesion) {
    return citaService
        .buscarMiCita(sesion)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }
}
