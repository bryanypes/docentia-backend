package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.SolicitudAnalisisResponse.HallazgoBorrador;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.analisis.ValidacionHallazgoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Revisión de los hallazgos de un borrador por coordinación. */
@RestController
@PreAuthorize("hasRole('COORDINADOR')")
@RequestMapping("/analisis-ia/hallazgos")
public class HallazgoController {

  private final ValidacionHallazgoService validacionService;

  public HallazgoController(ValidacionHallazgoService validacionService) {
    this.validacionService = validacionService;
  }

  /**
   * Marca un hallazgo como válido; 422 si no tiene ninguna cita verificada.
   *
   * @param idHallazgo identificador del hallazgo
   */
  @PostMapping("/{idHallazgo}/validacion")
  public HallazgoBorrador validar(
      @PathVariable Long idHallazgo, @AuthenticationPrincipal UsuarioSesion sesion) {
    return validacionService.validar(idHallazgo, sesion);
  }
}
