package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.DescarteHallazgoRequest;
import com.uniautonoma.docentia.dto.EdicionHallazgoRequest;
import com.uniautonoma.docentia.dto.SolicitudAnalisisResponse.HallazgoBorrador;
import com.uniautonoma.docentia.dto.VersionHallazgoResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.analisis.ValidacionHallazgoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

  /**
   * Excluye un hallazgo del resultado.
   *
   * @param idHallazgo identificador del hallazgo
   * @param solicitud motivo opcional
   */
  @PostMapping("/{idHallazgo}/descarte")
  public HallazgoBorrador descartar(
      @PathVariable Long idHallazgo,
      @Valid @RequestBody DescarteHallazgoRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return validacionService.descartar(idHallazgo, solicitud.motivo(), sesion);
  }

  /**
   * Corrige la redacción de un hallazgo.
   *
   * @param idHallazgo identificador del hallazgo
   * @param solicitud texto nuevo
   */
  @PutMapping("/{idHallazgo}")
  public HallazgoBorrador editar(
      @PathVariable Long idHallazgo,
      @Valid @RequestBody EdicionHallazgoRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return validacionService.editar(idHallazgo, solicitud.texto(), sesion);
  }

  /**
   * Historial de un hallazgo, con el modelo y la instrucción de cada versión.
   *
   * @param idHallazgo identificador del hallazgo
   */
  @GetMapping("/{idHallazgo}/versiones")
  public List<VersionHallazgoResponse> versiones(
      @PathVariable Long idHallazgo, @AuthenticationPrincipal UsuarioSesion sesion) {
    return validacionService.versiones(idHallazgo, sesion);
  }
}
