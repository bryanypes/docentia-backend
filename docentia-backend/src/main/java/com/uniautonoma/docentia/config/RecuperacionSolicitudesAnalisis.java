package com.uniautonoma.docentia.config;

import com.uniautonoma.docentia.repository.SolicitudAnalisisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Al arrancar devuelve a la cola las solicitudes que quedaron EN_PROCESO porque la aplicación se
 * detuvo a mitad de un análisis. Supone una sola instancia del backend procesando la cola.
 */
@Component
@ConditionalOnProperty(prefix = "docentia.ia.worker", name = "habilitado", havingValue = "true")
public class RecuperacionSolicitudesAnalisis implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(RecuperacionSolicitudesAnalisis.class);

  private final SolicitudAnalisisRepository solicitudRepository;

  public RecuperacionSolicitudesAnalisis(SolicitudAnalisisRepository solicitudRepository) {
    this.solicitudRepository = solicitudRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    int devueltas = solicitudRepository.devolverEnProcesoACola();
    if (devueltas > 0) {
      LOG.info("Se devolvieron {} solicitudes de análisis a la cola", devueltas);
    }
  }
}
