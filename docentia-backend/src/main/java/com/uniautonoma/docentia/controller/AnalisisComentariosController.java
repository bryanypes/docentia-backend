package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.AnalisisComentariosResponse;
import com.uniautonoma.docentia.dto.ComentariosEstudiantesResponse;
import com.uniautonoma.docentia.dto.ModelosAnalisisResponse;
import com.uniautonoma.docentia.dto.RegeneracionAnalisisRequest;
import com.uniautonoma.docentia.dto.ResumenComentariosResponse;
import com.uniautonoma.docentia.dto.SolicitudAnalisisResponse;
import com.uniautonoma.docentia.dto.UsoIaResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.analisis.AnalisisComentariosService;
import com.uniautonoma.docentia.service.analisis.AnalisisIaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Análisis de comentarios de estudiantes: resultado estructurado por docente, comentarios
 * anonimizados y resumen agregado. Los roles permitidos están en PoliticaComentarios; el docente no
 * entra (ve solo hallazgos validados en /analisis-ia/resultados/propio).
 */
@RestController
public class AnalisisComentariosController {

  private final AnalisisComentariosService servicio;
  private final AnalisisIaService analisisIaService;

  public AnalisisComentariosController(
      AnalisisComentariosService servicio, AnalisisIaService analisisIaService) {
    this.servicio = servicio;
    this.analisisIaService = analisisIaService;
  }

  /**
   * Vuelve a generar el análisis con un motivo. Respeta el enfriamiento y pasa por la caché: con
   * los mismos comentarios no gasta tokens.
   */
  @PostMapping("/analisis-comentarios/docentes/{docenteId}/regeneracion")
  @PreAuthorize("@politicaComentarios.puedeRegenerar(authentication)")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public SolicitudAnalisisResponse regenerar(
      @PathVariable Long docenteId,
      @Valid @RequestBody RegeneracionAnalisisRequest pedido,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return analisisIaService.regenerar(docenteId, pedido, sesion);
  }

  /** Modelos que se pueden elegir al pedir un análisis y cuánto consume el externo. */
  @GetMapping("/analisis-comentarios/modelos")
  @PreAuthorize("@politicaComentarios.puedeRegenerar(authentication)")
  public ModelosAnalisisResponse modelos(@AuthenticationPrincipal UsuarioSesion sesion) {
    return servicio.modelos(sesion);
  }

  /** Gasto del proveedor externo en el periodo. */
  @GetMapping("/analisis-comentarios/uso")
  @PreAuthorize("@politicaComentarios.puedeVerUso(authentication)")
  public UsoIaResponse uso(
      @RequestParam Long periodo, @AuthenticationPrincipal UsuarioSesion sesion) {
    return servicio.uso(periodo, sesion);
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
