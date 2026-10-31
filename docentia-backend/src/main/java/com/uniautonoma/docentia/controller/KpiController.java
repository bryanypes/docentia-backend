package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.KpisResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.KpiService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Indicadores generales para el dashboard de coordinación y vicerrectoría. */
@RestController
@PreAuthorize("hasAnyRole('COORDINADOR', 'VICERRECTOR')")
@RequestMapping("/kpis")
public class KpiController {

  private final KpiService kpiService;

  public KpiController(KpiService kpiService) {
    this.kpiService = kpiService;
  }

  /**
   * Calcula los cuatro indicadores del periodo.
   *
   * @param periodoId periodo consultado; sin él, el activo
   * @param facultadId filtro opcional del vicerrector; el coordinador siempre ve su facultad
   * @param sesion usuario autenticado
   * @return indicadores con valor anterior y variación
   */
  @GetMapping
  public KpisResponse obtenerKpis(
      @RequestParam(required = false) Long periodoId,
      @RequestParam(required = false) Long facultadId,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return kpiService.calcular(periodoId, facultadId, sesion);
  }
}
