package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.CitaProgramadaResponse;
import com.uniautonoma.docentia.service.AgendaAdministracionService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Agenda completa del periodo para la administración, de solo lectura. */
@RestController
public class AgendaAdministracionController {

  private final AgendaAdministracionService servicio;

  public AgendaAdministracionController(AgendaAdministracionService servicio) {
    this.servicio = servicio;
  }

  /** Citas activas: coordinación con docentes y vicerrectoría con coordinadores. */
  @GetMapping("/citas/programadas")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public List<CitaProgramadaResponse> citasProgramadas(
      @RequestParam(required = false) Long periodo) {
    return servicio.citasProgramadas(periodo);
  }
}
