package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.PeriodoAcademicoRequest;
import com.uniautonoma.docentia.dto.PeriodoAcademicoResponse;
import com.uniautonoma.docentia.service.PeriodoAcademicoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de los periodos académicos. */
@RestController
@RequestMapping("/periodos-academicos")
public class PeriodoAcademicoController {

  private final PeriodoAcademicoService periodoService;

  public PeriodoAcademicoController(PeriodoAcademicoService periodoService) {
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
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PeriodoAcademicoResponse crearPeriodo(
      @Valid @RequestBody PeriodoAcademicoRequest solicitud) {
    return periodoService.crearPeriodo(solicitud);
  }

  /**
   * Actualiza un periodo y sus etapas.
   *
   * @param idPeriodo identificador del periodo
   * @param solicitud datos nuevos
   * @return el periodo actualizado
   */
  @PutMapping("/{idPeriodo}")
  public PeriodoAcademicoResponse actualizarPeriodo(
      @PathVariable Long idPeriodo, @Valid @RequestBody PeriodoAcademicoRequest solicitud) {
    return periodoService.actualizarPeriodo(idPeriodo, solicitud);
  }

  /**
   * Activa un periodo; el que estuviera activo deja de estarlo.
   *
   * @param idPeriodo identificador del periodo
   * @return el periodo activado
   */
  @PutMapping("/{idPeriodo}/activacion")
  public PeriodoAcademicoResponse activarPeriodo(@PathVariable Long idPeriodo) {
    return periodoService.activarPeriodo(idPeriodo);
  }

  /**
   * Desactiva un periodo.
   *
   * @param idPeriodo identificador del periodo
   */
  @DeleteMapping("/{idPeriodo}/activacion")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void desactivarPeriodo(@PathVariable Long idPeriodo) {
    periodoService.desactivarPeriodo(idPeriodo);
  }
}
