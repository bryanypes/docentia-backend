package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.SolicitudAnalisisRequest;
import com.uniautonoma.docentia.dto.SolicitudAnalisisResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.analisis.AnalisisIaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Análisis de los comentarios de estudiantes con IA. El borrador nunca se expone al docente: solo
 * coordinación (con alcance) y vicerrectoría lo consultan mientras no esté validado.
 */
@RestController
@RequestMapping("/analisis-ia/solicitudes")
public class AnalisisIaController {

  private final AnalisisIaService analisisService;

  public AnalisisIaController(AnalisisIaService analisisService) {
    this.analisisService = analisisService;
  }

  /**
   * Registra una solicitud; el análisis ocurre después, en segundo plano.
   *
   * @return la solicitud en EN_COLA, con 202
   */
  @PostMapping
  @PreAuthorize("hasRole('COORDINADOR')")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public SolicitudAnalisisResponse solicitar(
      @Valid @RequestBody SolicitudAnalisisRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return analisisService.solicitar(solicitud, sesion);
  }

  /**
   * Estado de la solicitud y, si terminó, su borrador.
   *
   * @param idSolicitud identificador de la solicitud
   */
  @GetMapping("/{idSolicitud}")
  @PreAuthorize("hasAnyRole('COORDINADOR', 'VICERRECTOR')")
  public SolicitudAnalisisResponse consultar(
      @PathVariable Long idSolicitud, @AuthenticationPrincipal UsuarioSesion sesion) {
    return analisisService.consultar(idSolicitud, sesion);
  }

  /**
   * Solicitudes de un docente en un periodo, la más reciente primero.
   *
   * @param docenteId docente consultado
   * @param periodoId periodo consultado
   */
  @GetMapping
  @PreAuthorize("hasAnyRole('COORDINADOR', 'VICERRECTOR')")
  public List<SolicitudAnalisisResponse> listar(
      @RequestParam Long docenteId,
      @RequestParam Long periodoId,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return analisisService.listar(docenteId, periodoId, sesion);
  }
}
