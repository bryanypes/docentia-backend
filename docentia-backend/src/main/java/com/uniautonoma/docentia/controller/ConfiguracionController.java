package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.config.ActasProperties;
import com.uniautonoma.docentia.dto.ConfiguracionEvaluacionResponse;
import com.uniautonoma.docentia.entity.PesosFuentes;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reglas que la aplicación web muestra tal como están configuradas en el servidor. */
@RestController
@RequestMapping("/configuracion")
public class ConfiguracionController {

  private final ActasProperties propiedades;
  private final BigDecimal umbralRevision;

  public ConfiguracionController(
      ActasProperties propiedades,
      @Value("${docentia.revision.umbral-promedio:4.0}") BigDecimal umbralRevision) {
    this.propiedades = propiedades;
    this.umbralRevision = umbralRevision;
  }

  @GetMapping("/evaluacion")
  public ConfiguracionEvaluacionResponse obtener() {
    PesosFuentes pesos = PesosFuentes.predeterminados();
    return new ConfiguracionEvaluacionResponse(
        propiedades.compromisos().umbral(),
        umbralRevision,
        propiedades.actas().rolCorrector(),
        pesos.pesoEstudiantes(),
        pesos.pesoCoordinacion(),
        pesos.pesoAutoevaluacion());
  }
}
