package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.PanoramaAnalisisResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.analisis.PanoramaAnalisisService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Panorama del análisis de comentarios del alcance, solo con conteos. */
@RestController
public class PanoramaAnalisisController {

  private final PanoramaAnalisisService panoramaService;

  public PanoramaAnalisisController(PanoramaAnalisisService panoramaService) {
    this.panoramaService = panoramaService;
  }

  @GetMapping("/analisis-ia/panorama")
  @PreAuthorize("hasAnyRole('COORDINADOR', 'VICERRECTOR', 'ADMINISTRADOR')")
  public PanoramaAnalisisResponse panorama(
      @RequestParam(required = false) Long periodoId,
      @RequestParam(required = false) Long idFacultad,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return panoramaService.panorama(periodoId, idFacultad, sesion);
  }
}
