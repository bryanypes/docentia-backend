package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.DecisionReaperturaRequest;
import com.uniautonoma.docentia.dto.SolicitudReaperturaRequest;
import com.uniautonoma.docentia.dto.SolicitudReaperturaResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.formulario.ReaperturaFormularioService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Solicitudes para corregir una autoevaluación o coevaluación ya enviada. */
@RestController
@RequestMapping("/solicitudes-reapertura")
public class ReaperturaController {

  private final ReaperturaFormularioService servicio;

  public ReaperturaController(ReaperturaFormularioService servicio) {
    this.servicio = servicio;
  }

  /** Pide corregir la propia autoevaluación o una coevaluación que uno mismo envió. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('DOCENTE', 'COORDINADOR', 'VICERRECTOR')")
  public SolicitudReaperturaResponse solicitar(
      @Valid @RequestBody SolicitudReaperturaRequest pedido,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return servicio.solicitar(pedido, sesion);
  }

  @GetMapping("/mias")
  @PreAuthorize("hasAnyRole('DOCENTE', 'COORDINADOR', 'VICERRECTOR')")
  public List<SolicitudReaperturaResponse> listarPropias(
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return servicio.listarPropias(sesion);
  }

  @GetMapping("/por-verificar")
  @PreAuthorize("hasAnyRole('COORDINADOR', 'VICERRECTOR')")
  public List<SolicitudReaperturaResponse> listarPorVerificar(
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return servicio.listarPorVerificar(sesion);
  }

  /** El superior aprueba o rechaza. */
  @PostMapping("/{idSolicitud}/verificacion")
  @PreAuthorize("hasAnyRole('COORDINADOR', 'VICERRECTOR')")
  public SolicitudReaperturaResponse verificar(
      @PathVariable Long idSolicitud,
      @Valid @RequestBody DecisionReaperturaRequest decision,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return servicio.verificar(idSolicitud, decision, sesion);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public List<SolicitudReaperturaResponse> listarTodas() {
    return servicio.listarTodas();
  }

  /** La administración reabre el formulario o rechaza la solicitud. */
  @PostMapping("/{idSolicitud}/atencion")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public SolicitudReaperturaResponse atender(
      @PathVariable Long idSolicitud,
      @Valid @RequestBody DecisionReaperturaRequest decision,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return servicio.atender(idSolicitud, decision, sesion);
  }
}
