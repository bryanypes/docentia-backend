package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.AsistenciaCitaRequest;
import com.uniautonoma.docentia.dto.CitaResponse;
import com.uniautonoma.docentia.dto.ConfirmacionCitaRequest;
import com.uniautonoma.docentia.dto.ProcesoEvaluacionResponse;
import com.uniautonoma.docentia.dto.ReprogramacionCitaRequest;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.AsistenciaCitaService;
import com.uniautonoma.docentia.service.GestionCitaService;
import com.uniautonoma.docentia.service.ProcesoEvaluacionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Estado general del proceso de evaluación, confirmación de la cita y asistencia a la reunión. */
@RestController
public class ProcesoEvaluacionController {

  private final ProcesoEvaluacionService procesoService;
  private final AsistenciaCitaService asistenciaService;
  private final GestionCitaService gestionCitaService;

  public ProcesoEvaluacionController(
      ProcesoEvaluacionService procesoService,
      AsistenciaCitaService asistenciaService,
      GestionCitaService gestionCitaService) {
    this.procesoService = procesoService;
    this.asistenciaService = asistenciaService;
    this.gestionCitaService = gestionCitaService;
  }

  /** Proceso de los docentes del alcance en el periodo activo. */
  @GetMapping("/procesos-evaluacion")
  @PreAuthorize("hasAnyRole('COORDINADOR', 'VICERRECTOR')")
  public List<ProcesoEvaluacionResponse> listar(
      @RequestParam(required = false) Long idFacultad,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return procesoService.listar(idFacultad, sesion);
  }

  /** Proceso del docente autenticado; mientras las evaluaciones estén en curso, sin detalle. */
  @GetMapping("/procesos-evaluacion/propio")
  @PreAuthorize("hasRole('DOCENTE')")
  public ProcesoEvaluacionResponse propio(@AuthenticationPrincipal UsuarioSesion sesion) {
    return procesoService.obtenerPropio(sesion);
  }

  /** Proceso de un docente con su línea de tiempo. */
  @GetMapping("/procesos-evaluacion/{idDocente}")
  @PreAuthorize("hasAnyRole('COORDINADOR', 'VICERRECTOR')")
  public ProcesoEvaluacionResponse obtener(
      @PathVariable Long idDocente, @AuthenticationPrincipal UsuarioSesion sesion) {
    return procesoService.obtener(idDocente, sesion);
  }

  /** El docente confirma o declina su cita activa. */
  @PostMapping("/citas/mia/confirmacion")
  @PreAuthorize("hasRole('DOCENTE')")
  public CitaResponse confirmar(
      @Valid @RequestBody ConfirmacionCitaRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return asistenciaService.responderConfirmacion(solicitud.asiste(), sesion);
  }

  /** La coordinación registra si el docente asistió a la reunión. */
  @PostMapping("/citas/{idCita}/asistencia")
  @PreAuthorize("hasRole('COORDINADOR')")
  public CitaResponse registrarAsistencia(
      @PathVariable Long idCita,
      @Valid @RequestBody AsistenciaCitaRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return asistenciaService.registrarAsistencia(idCita, solicitud.asistio(), sesion);
  }

  /** La coordinación cancela una cita de sus franjas; la franja queda libre. */
  @PostMapping("/citas/{idCita}/cancelacion")
  @PreAuthorize("hasRole('COORDINADOR')")
  public CitaResponse cancelar(
      @PathVariable Long idCita, @AuthenticationPrincipal UsuarioSesion sesion) {
    return gestionCitaService.cancelar(idCita, sesion);
  }

  /** La coordinación mueve una cita a otra de sus franjas libres. */
  @PostMapping("/citas/{idCita}/reprogramacion")
  @PreAuthorize("hasRole('COORDINADOR')")
  public CitaResponse reprogramar(
      @PathVariable Long idCita,
      @Valid @RequestBody ReprogramacionCitaRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return gestionCitaService.reprogramar(idCita, solicitud.idFranja(), sesion);
  }
}
