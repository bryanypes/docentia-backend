package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.PonderacionRequest;
import com.uniautonoma.docentia.dto.PonderacionResponse;
import com.uniautonoma.docentia.service.PonderacionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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

  public PonderacionController(PonderacionService ponderacionService) {
    this.ponderacionService = ponderacionService;
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
      @Valid @RequestBody PonderacionRequest solicitud) {
    return ponderacionService.configurarPonderacionActiva(solicitud);
  }
}
