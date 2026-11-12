package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.CumplimientoRequest;
import com.uniautonoma.docentia.dto.PlanSeguimientoResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.PlanSeguimientoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Seguimiento de los compromisos del periodo anterior. */
@RestController
public class PlanSeguimientoController {

  private final PlanSeguimientoService planService;

  public PlanSeguimientoController(PlanSeguimientoService planService) {
    this.planService = planService;
  }

  @GetMapping("/docentes/{idDocente}/plan-seguimiento")
  @PreAuthorize("hasAnyRole('COORDINADOR', 'VICERRECTOR', 'ADMINISTRADOR')")
  public PlanSeguimientoResponse consultar(
      @PathVariable Long idDocente, @AuthenticationPrincipal UsuarioSesion sesion) {
    return planService.consultar(idDocente, sesion);
  }

  @GetMapping("/plan-seguimiento/propio")
  @PreAuthorize("hasRole('DOCENTE')")
  public PlanSeguimientoResponse consultarPropio(@AuthenticationPrincipal UsuarioSesion sesion) {
    return planService.consultarPropio(sesion);
  }

  @PutMapping("/compromisos/{idCompromiso}/cumplimiento")
  @PreAuthorize("hasRole('COORDINADOR')")
  public PlanSeguimientoResponse marcarCumplido(
      @PathVariable Long idCompromiso,
      @Valid @RequestBody CumplimientoRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return planService.marcarCumplido(idCompromiso, solicitud.observacion(), sesion);
  }
}
