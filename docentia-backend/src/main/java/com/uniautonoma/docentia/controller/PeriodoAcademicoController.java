package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.CalendarioResponse;
import com.uniautonoma.docentia.dto.PeriodoAcademicoRequest;
import com.uniautonoma.docentia.dto.PeriodoAcademicoResponse;
import com.uniautonoma.docentia.dto.VentanaEtapaResponse;
import com.uniautonoma.docentia.entity.AccionAuditoria;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.AuditoriaService;
import com.uniautonoma.docentia.service.PeriodoAcademicoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de los periodos académicos. */
@RestController
@RequestMapping("/periodos-academicos")
public class PeriodoAcademicoController {

  private final PeriodoAcademicoService periodoService;
  private final AuditoriaService auditoria;

  public PeriodoAcademicoController(
      PeriodoAcademicoService periodoService, AuditoriaService auditoria) {
    this.auditoria = auditoria;
    this.periodoService = periodoService;
  }

  /**
   * Lista los periodos académicos.
   *
   * @return periodos del más reciente al más antiguo
   */
  @GetMapping
  public List<PeriodoAcademicoResponse> listarPeriodos() {
    return periodoService.listarPeriodos();
  }

  /**
   * Consulta el periodo activo.
   *
   * @return el periodo que está activo
   */
  @GetMapping("/activo")
  public PeriodoAcademicoResponse obtenerPeriodoActivo() {
    return periodoService.obtenerPeriodoActivo();
  }

  /**
   * Dice qué etapas del periodo activo están abiertas hoy, para habilitar o no cada acción.
   *
   * @return las cuatro etapas con sus fechas y si están abiertas
   */
  @GetMapping("/activo/calendario")
  public CalendarioResponse obtenerCalendario() {
    return periodoService.describirCalendario();
  }

  @GetMapping("/activo/ventanas")
  public List<VentanaEtapaResponse> listarVentanasActivas() {
    return periodoService.describirVentanasActivas();
  }

  /**
   * Consulta un periodo.
   *
   * @param idPeriodo identificador del periodo
   * @return el periodo solicitado
   */
  @GetMapping("/{idPeriodo}")
  public PeriodoAcademicoResponse obtenerPeriodo(@PathVariable Long idPeriodo) {
    return periodoService.obtenerPeriodo(idPeriodo);
  }

  /**
   * Crea un periodo con sus etapas.
   *
   * @param solicitud datos del periodo
   * @return el periodo creado, todavía inactivo
   */
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PeriodoAcademicoResponse crearPeriodo(
      @Valid @RequestBody PeriodoAcademicoRequest solicitud) {
    return periodoService.crearPeriodo(solicitud);
  }

  /**
   * Actualiza un periodo y sus etapas. Solo el ADMINISTRADOR puede hacer cambios;
   * el VICERRECTOR puede visualizar.
   *
   * @param idPeriodo identificador del periodo
   * @param solicitud datos nuevos
   * @return el periodo actualizado
   */
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  @PutMapping("/{idPeriodo}")
  public PeriodoAcademicoResponse actualizarPeriodo(
      @PathVariable Long idPeriodo,
      @Valid @RequestBody PeriodoAcademicoRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    PeriodoAcademicoResponse periodo = periodoService.actualizarPeriodo(idPeriodo, solicitud);
    auditoria.registrar(AccionAuditoria.CAMBIO_VENTANA, "PERIODO", idPeriodo, sesion);
    return periodo;
  }

  /**
   * Activa un periodo; el que estuviera activo deja de estarlo. Solo el ADMINISTRADOR.
   *
   * @param idPeriodo identificador del periodo
   * @param confirmar acepta activar el periodo aunque no tenga matrículas
   * @return el periodo activado
   */
  @PreAuthorize("hasAnyRole('VICERRECTOR', 'ADMINISTRADOR')")
  @PutMapping("/{idPeriodo}/activacion")
  public PeriodoAcademicoResponse activarPeriodo(
      @PathVariable Long idPeriodo,
      @RequestParam(defaultValue = "false") boolean confirmar,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    PeriodoAcademicoResponse periodo = periodoService.activarPeriodo(idPeriodo, confirmar);
    auditoria.registrar(AccionAuditoria.ACTIVACION_PERIODO, "PERIODO", idPeriodo, sesion);
    return periodo;
  }

  /**
   * Desactiva un periodo. Solo el ADMINISTRADOR.
   *
   * @param idPeriodo identificador del periodo
   */
  @PreAuthorize("hasAnyRole('VICERRECTOR', 'ADMINISTRADOR')")
  @DeleteMapping("/{idPeriodo}/activacion")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void desactivarPeriodo(
      @PathVariable Long idPeriodo, @AuthenticationPrincipal UsuarioSesion sesion) {
    periodoService.desactivarPeriodo(idPeriodo);
    auditoria.registrar(AccionAuditoria.DESACTIVACION_PERIODO, "PERIODO", idPeriodo, sesion);
  }
}
