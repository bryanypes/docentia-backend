package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.PaginaResponse;
import com.uniautonoma.docentia.dto.RegistroAuditoriaResponse;
import com.uniautonoma.docentia.entity.AccionAuditoria;
import com.uniautonoma.docentia.service.AuditoriaService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de la auditoría, de solo lectura. */
@RestController
@PreAuthorize("hasAnyRole('VICERRECTOR', 'ADMINISTRADOR')")
@RequestMapping("/auditoria")
public class AuditoriaController {

  private final AuditoriaService auditoriaService;

  public AuditoriaController(AuditoriaService auditoriaService) {
    this.auditoriaService = auditoriaService;
  }

  @GetMapping
  public PaginaResponse<RegistroAuditoriaResponse> consultar(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate hasta,
      @RequestParam(required = false) AccionAuditoria accion,
      @RequestParam(required = false) String usuario,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return auditoriaService.consultar(desde, hasta, accion, usuario, page, size);
  }
}
