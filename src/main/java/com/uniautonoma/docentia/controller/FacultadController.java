package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.FacultadRequest;
import com.uniautonoma.docentia.dto.FacultadResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.FacultadService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de administración de facultades. */
@RestController
@PreAuthorize("hasAnyRole('VICERRECTOR', 'COORDINADOR')")
@RequestMapping("/facultades")
public class FacultadController {

  private final FacultadService facultadService;

  public FacultadController(FacultadService facultadService) {
    this.facultadService = facultadService;
  }

  /**
   * Lista las facultades vigentes dentro del alcance del usuario.
   *
   * @param sesion usuario autenticado
   * @return facultades activas
   */
  @GetMapping
  public List<FacultadResponse> listarFacultades(@AuthenticationPrincipal UsuarioSesion sesion) {
    return facultadService.listarFacultades(sesion);
  }

  /**
   * Consulta una facultad.
   *
   * @param idFacultad identificador de la facultad
   * @param sesion usuario autenticado
   * @return la facultad solicitada
   */
  @GetMapping("/{idFacultad}")
  public FacultadResponse obtenerFacultad(
      @PathVariable Long idFacultad, @AuthenticationPrincipal UsuarioSesion sesion) {
    return facultadService.obtenerFacultad(idFacultad, sesion);
  }

  /**
   * Crea una facultad.
   *
   * @param solicitud datos de la facultad
   * @return la facultad creada
   */
  @PreAuthorize("hasRole('VICERRECTOR')")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FacultadResponse crearFacultad(@Valid @RequestBody FacultadRequest solicitud) {
    return facultadService.crearFacultad(solicitud);
  }

  /**
   * Actualiza una facultad.
   *
   * @param idFacultad identificador de la facultad
   * @param solicitud datos nuevos
   * @return la facultad actualizada
   */
  @PreAuthorize("hasRole('VICERRECTOR')")
  @PutMapping("/{idFacultad}")
  public FacultadResponse actualizarFacultad(
      @PathVariable Long idFacultad, @Valid @RequestBody FacultadRequest solicitud) {
    return facultadService.actualizarFacultad(idFacultad, solicitud);
  }

  /**
   * Da de baja una facultad que no tenga programas activos.
   *
   * @param idFacultad identificador de la facultad
   */
  @PreAuthorize("hasRole('VICERRECTOR')")
  @DeleteMapping("/{idFacultad}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminarFacultad(@PathVariable Long idFacultad) {
    facultadService.eliminarFacultad(idFacultad);
  }
}
