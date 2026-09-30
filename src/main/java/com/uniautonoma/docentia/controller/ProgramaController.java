package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.ProgramaRequest;
import com.uniautonoma.docentia.dto.ProgramaResponse;
import com.uniautonoma.docentia.service.ProgramaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de administración de programas académicos. */
@RestController
@RequestMapping("/programas")
public class ProgramaController {

  private final ProgramaService programaService;

  public ProgramaController(ProgramaService programaService) {
    this.programaService = programaService;
  }

  /**
   * Lista los programas vigentes.
   *
   * @param idFacultad filtro opcional por facultad
   * @return programas activos
   */
  @GetMapping
  public List<ProgramaResponse> listarProgramas(@RequestParam(required = false) Long idFacultad) {
    return programaService.listarProgramas(idFacultad);
  }

  /**
   * Consulta un programa.
   *
   * @param idPrograma identificador del programa
   * @return el programa solicitado
   */
  @GetMapping("/{idPrograma}")
  public ProgramaResponse obtenerPrograma(@PathVariable Long idPrograma) {
    return programaService.obtenerPrograma(idPrograma);
  }

  /**
   * Crea un programa dentro de una facultad.
   *
   * @param solicitud datos del programa
   * @return el programa creado
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProgramaResponse crearPrograma(@Valid @RequestBody ProgramaRequest solicitud) {
    return programaService.crearPrograma(solicitud);
  }

  /**
   * Actualiza un programa.
   *
   * @param idPrograma identificador del programa
   * @param solicitud datos nuevos
   * @return el programa actualizado
   */
  @PutMapping("/{idPrograma}")
  public ProgramaResponse actualizarPrograma(
      @PathVariable Long idPrograma, @Valid @RequestBody ProgramaRequest solicitud) {
    return programaService.actualizarPrograma(idPrograma, solicitud);
  }

  /**
   * Da de baja un programa.
   *
   * @param idPrograma identificador del programa
   */
  @DeleteMapping("/{idPrograma}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminarPrograma(@PathVariable Long idPrograma) {
    programaService.eliminarPrograma(idPrograma);
  }
}
