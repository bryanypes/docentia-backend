package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.VerificacionActaResponse;
import com.uniautonoma.docentia.service.ActaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Verificación pública de un acta con el código impreso en el PDF. */
@RestController
@RequestMapping("/verificacion-actas")
public class VerificacionActaController {

  private final ActaService actaService;

  public VerificacionActaController(ActaService actaService) {
    this.actaService = actaService;
  }

  @GetMapping("/{codigo}")
  public VerificacionActaResponse verificar(@PathVariable String codigo) {
    return actaService.verificar(codigo);
  }
}
