package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.PonderacionRequest;
import com.uniautonoma.docentia.dto.PonderacionResponse;
import com.uniautonoma.docentia.entity.AccionAuditoria;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.AuditoriaService;
import com.uniautonoma.docentia.service.PonderacionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de las ponderaciones del periodo activo. */
@RestController
@RequestMapping("/periodos-academicos/activo/ponderaciones")
public class PonderacionController {

  private final PonderacionService ponderacionService;
  private final AuditoriaService auditoria;

  public PonderacionController(PonderacionService ponderacionService, AuditoriaService auditoria) {
    this.ponderacionService = ponderacionService;
    this.auditoria = auditoria;
  }

  /**
   * Consulta los pesos con los que se pondera cada fuente en el periodo activo.
   *
   * @return pesos vigentes e indicación de si son los predeterminados
   */
  @GetMapping
  @PreAuthorize("hasAnyRole('VICERRECTOR', 'COORDINADOR')")
  public PonderacionResponse obtenerPonderacionActiva() {
    return ponderacionService.obtenerPonderacionActiva();
  }

  /**
   * Configura los pesos de las tres fuentes para el periodo activo.
   *
   * @param solicitud pesos que deben sumar 100
   * @return la ponderación guardada
   */
  @PutMapping
  @PreAuthorize("hasRole('VICERRECTOR')")
  public PonderacionResponse configurarPonderacionActiva(
      @Valid @RequestBody PonderacionRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    PonderacionResponse guardada = ponderacionService.configurarPonderacionActiva(solicitud);
    auditoria.registrar(AccionAuditoria.CAMBIO_PESOS, "PONDERACION", null, sesion);
    return guardada;
  }
}
