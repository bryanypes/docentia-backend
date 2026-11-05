package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.AnalisisComentariosResponse;
import com.uniautonoma.docentia.dto.ComentariosEstudiantesResponse;
import com.uniautonoma.docentia.dto.ResumenComentariosResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.analisis.AnalisisComentariosService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Análisis de comentarios de estudiantes: resultado estructurado por docente, comentarios
 * anonimizados y resumen agregado. Los roles permitidos están en PoliticaComentarios; el docente no
 * entra (ve solo hallazgos validados en /analisis-ia/resultados/propio).
 */
@RestController
public class AnalisisComentariosController {

  private final AnalisisComentariosService servicio;

  public AnalisisComentariosController(AnalisisComentariosService servicio) {
    this.servicio = servicio;
  }

  @GetMapping("/analisis-comentarios/docentes/{docenteId}")
  @PreAuthorize("@politicaComentarios.puedeVerAnalisis(authentication)")
  public AnalisisComentariosResponse analisis(
      @PathVariable Long docenteId,
      @RequestParam Long periodo,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return servicio.analisis(docenteId, periodo, sesion);
  }

  @GetMapping("/comentarios-estudiantes/docentes/{docenteId}")
  @PreAuthorize("@politicaComentarios.puedeVerComentarios(authentication)")
  public ComentariosEstudiantesResponse comentarios(
      @PathVariable Long docenteId,
      @RequestParam Long periodo,
      @RequestParam(required = false) String criterio,
      @RequestParam(required = false) String sentimiento,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return servicio.comentarios(docenteId, periodo, criterio, sentimiento, page, size, sesion);
  }

  @GetMapping("/analisis-comentarios/resumen")
  @PreAuthorize("@politicaComentarios.puedeVerResumen(authentication)")
  public ResumenComentariosResponse resumen(
      @RequestParam Long periodo,
      @RequestParam(required = false) Long programa,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return servicio.resumen(periodo, programa, sesion);
  }
}
