package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.ResultadoAnalisisPropioResponse;
import com.uniautonoma.docentia.dto.SolicitudAnalisisResponse;
import com.uniautonoma.docentia.entity.AccionAuditoria;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.AuditoriaService;
import com.uniautonoma.docentia.service.analisis.ValidacionHallazgoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publicación de la revisión y consulta del resultado por el docente. */
@RestController
@RequestMapping("/analisis-ia")
public class ResultadoAnalisisController {

  private final ValidacionHallazgoService validacionService;
  private final AuditoriaService auditoria;

  public ResultadoAnalisisController(
      ValidacionHallazgoService validacionService, AuditoriaService auditoria) {
    this.validacionService = validacionService;
    this.auditoria = auditoria;
  }

  /**
   * Publica la revisión; 409 si quedan hallazgos sin decidir o no se incluyó ninguno.
   *
   * @param idSolicitud identificador de la solicitud
   */
  @PostMapping("/solicitudes/{idSolicitud}/publicacion")
  @PreAuthorize("hasRole('COORDINADOR')")
  public SolicitudAnalisisResponse publicar(
      @PathVariable Long idSolicitud, @AuthenticationPrincipal UsuarioSesion sesion) {
    SolicitudAnalisisResponse publicada = validacionService.publicar(idSolicitud, sesion);
    auditoria.registrar(
        AccionAuditoria.PUBLICACION_HALLAZGOS, "SOLICITUD_ANALISIS", idSolicitud, sesion);
    return publicada;
  }

  /** Hallazgos incluidos en la última revisión publicada del docente autenticado. */
  @GetMapping("/resultados/propio")
  @PreAuthorize("hasRole('DOCENTE')")
  public ResultadoAnalisisPropioResponse resultadoPropio(
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return validacionService.resultadoPropio(sesion);
  }
}
