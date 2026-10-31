package com.uniautonoma.docentia.config;

import com.uniautonoma.docentia.service.PromedioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, calcula los promedios de las calificaciones que todavía no tienen uno guardado.
 * Cubre las bases que ya tenían calificaciones cuando se creó la tabla de promedios; en una base al
 * día no encuentra nada.
 */
@Component
public class CalculoPromediosPendientes implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(CalculoPromediosPendientes.class);

  private final PromedioService promedioService;

  public CalculoPromediosPendientes(PromedioService promedioService) {
    this.promedioService = promedioService;
  }

  @Override
  public void run(ApplicationArguments args) {
    int calculados = promedioService.recalcularPendientes();
    if (calculados > 0) {
      LOG.info("Se calcularon {} promedios que no estaban guardados", calculados);
    }
  }
}
