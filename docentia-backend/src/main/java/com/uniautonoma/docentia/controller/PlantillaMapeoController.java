package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.PlantillaMapeoRequest;
import com.uniautonoma.docentia.dto.PlantillaMapeoResponse;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.importacion.PlantillaMapeoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Plantillas de mapeo de columnas del CSV; cada coordinador ve y maneja solo las suyas. */
@RestController
@PreAuthorize("hasRole('COORDINADOR')")
@RequestMapping("/importaciones/plantillas")
public class PlantillaMapeoController {

  private final PlantillaMapeoService plantillaService;

  public PlantillaMapeoController(PlantillaMapeoService plantillaService) {
    this.plantillaService = plantillaService;
  }

  /**
   * Lista las plantillas propias.
   *
   * @return de la más reciente a la más antigua
   */
  @GetMapping
  public List<PlantillaMapeoResponse> listarPlantillas(
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return plantillaService.listar(sesion);
  }

  /**
   * Guarda una plantilla; si ya hay una con ese nombre, la reemplaza.
   *
   * @param solicitud nombre, columnas del archivo y mapeo
   * @return la plantilla guardada
   */
  @PostMapping
  public PlantillaMapeoResponse guardarPlantilla(
      @Valid @RequestBody PlantillaMapeoRequest solicitud,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return plantillaService.guardar(solicitud, sesion);
  }

  /**
   * Borra una plantilla propia.
   *
   * @param idPlantilla identificador de la plantilla
   */
  @DeleteMapping("/{idPlantilla}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminarPlantilla(
      @PathVariable Long idPlantilla, @AuthenticationPrincipal UsuarioSesion sesion) {
    plantillaService.eliminar(idPlantilla, sesion);
  }
}
