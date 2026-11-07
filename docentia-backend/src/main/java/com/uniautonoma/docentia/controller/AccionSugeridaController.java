package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.AccionSugeridaResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.AccionSugeridaService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Acciones institucionales sugeridas por categoría de desempeño. */
@RestController
@PreAuthorize("hasAnyRole('COORDINADOR', 'VICERRECTOR', 'ADMINISTRADOR')")
@RequestMapping("/acciones-sugeridas")
public class AccionSugeridaController {

  private final AccionSugeridaService accionService;

  public AccionSugeridaController(AccionSugeridaService accionService) {
    this.accionService = accionService;
  }

  /**
   * Lista las acciones de un periodo.
   *
   * @param periodo identificador del periodo, o el activo si se omite
   * @param sesion usuario autenticado
   */
  @GetMapping
  public List<AccionSugeridaResponse> listar(
      @RequestParam(required = false) Long periodo, @AuthenticationPrincipal UsuarioSesion sesion) {
    return accionService.listar(periodo, sesion);
  }
}
