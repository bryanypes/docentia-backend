package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.DocenteResponse;
import com.uniautonoma.docentia.dto.SeguimientoDocentesResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.DocenteService;
import com.uniautonoma.docentia.service.SeguimientoDocentesService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de consulta de docentes para coordinación y vicerrectoría. */
@RestController
@PreAuthorize("hasAnyRole('VICERRECTOR', 'COORDINADOR')")
@RequestMapping("/docentes")
public class DocenteController {

  private final DocenteService docenteService;
  private final SeguimientoDocentesService seguimientoService;

  public DocenteController(
      DocenteService docenteService, SeguimientoDocentesService seguimientoService) {
    this.docenteService = docenteService;
    this.seguimientoService = seguimientoService;
  }

  /**
   * Lista los docentes del alcance con su promedio del periodo activo.
   *
   * @param idFacultad filtro opcional por facultad; se ignora para el coordinador
   * @param sesion usuario autenticado
   * @return periodo activo y docentes con su promedio
   */
  @GetMapping("/seguimiento")
  public SeguimientoDocentesResponse listarSeguimiento(
      @RequestParam(required = false) Long idFacultad,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return seguimientoService.listar(idFacultad, sesion);
  }

  /**
   * Lista los docentes dentro del alcance del usuario.
   *
   * @param idFacultad filtro opcional por facultad; se ignora para el coordinador
   * @param sesion usuario autenticado
   * @return docentes activos
   */
  @GetMapping
  public List<DocenteResponse> listarDocentes(
      @RequestParam(required = false) Long idFacultad,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return docenteService.listarDocentes(idFacultad, sesion);
  }

  /**
   * Consulta un docente.
   *
   * @param idDocente identificador del docente
   * @param sesion usuario autenticado
   * @return el docente solicitado
   */
  @GetMapping("/{idDocente}")
  public DocenteResponse obtenerDocente(
      @PathVariable Long idDocente, @AuthenticationPrincipal UsuarioSesion sesion) {
    return docenteService.obtenerDocente(idDocente, sesion);
  }
}
