package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.ExpedientePropioResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.ExpedienteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints del expediente de evaluación docente. */
@RestController
@RequestMapping("/expedientes")
public class ExpedienteController {

  private final ExpedienteService expedienteService;

  public ExpedienteController(ExpedienteService expedienteService) {
    this.expedienteService = expedienteService;
  }

  /**
   * Devuelve el expediente del docente autenticado. El docente se toma de la sesión, no de la URL,
   * de modo que nadie puede pedir el expediente de otro.
   *
   * @param sesion docente autenticado
   * @return expediente sin las notas individuales de coordinación ni de vicerrectoría
   */
  @GetMapping("/propio")
  @PreAuthorize("hasRole('DOCENTE')")
  public ExpedientePropioResponse obtenerExpedientePropio(
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return expedienteService.obtenerExpedientePropio(sesion);
  }
}
