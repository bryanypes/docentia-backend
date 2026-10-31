package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.config.ActasProperties;
import com.uniautonoma.docentia.dto.ConfiguracionEvaluacionResponse;
import com.uniautonoma.docentia.entity.PesosFuentes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reglas que la aplicación web muestra tal como están configuradas en el servidor. */
@RestController
@RequestMapping("/configuracion")
public class ConfiguracionController {

  private final ActasProperties propiedades;

  public ConfiguracionController(ActasProperties propiedades) {
    this.propiedades = propiedades;
  }

  @GetMapping("/evaluacion")
  public ConfiguracionEvaluacionResponse obtener() {
    PesosFuentes pesos = PesosFuentes.predeterminados();
    return new ConfiguracionEvaluacionResponse(
        propiedades.compromisos().umbral(),
        propiedades.actas().rolCorrector(),
        pesos.pesoEstudiantes(),
        pesos.pesoCoordinacion(),
        pesos.pesoAutoevaluacion());
  }
}
