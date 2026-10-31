package com.uniautonoma.docentia.service.notificacion;

import com.uniautonoma.docentia.config.NotificacionProperties;
import com.uniautonoma.docentia.repository.NotificacionRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Tareas periódicas. Las pruebas las invocan a mano, sin programador. */
@Component
@ConditionalOnProperty(
    prefix = "docentia.notificaciones.programador",
    name = "habilitado",
    havingValue = "true",
    matchIfMissing = true)
class TareasNotificacion {

  private static final Logger LOG = LoggerFactory.getLogger(TareasNotificacion.class);

  private final ProcesadorColaCorreo colaCorreo;
  private final GeneradorRecordatorios recordatorios;
  private final NotificacionRepository notificacionRepository;
  private final NotificacionProperties propiedades;
  private final Clock reloj;

  TareasNotificacion(
      ProcesadorColaCorreo colaCorreo,
      GeneradorRecordatorios recordatorios,
      NotificacionRepository notificacionRepository,
      NotificacionProperties propiedades,
      Clock reloj) {
    this.colaCorreo = colaCorreo;
    this.recordatorios = recordatorios;
    this.notificacionRepository = notificacionRepository;
    this.propiedades = propiedades;
    this.reloj = reloj;
  }

  @Scheduled(fixedDelayString = "${docentia.notificaciones.programador.intervalo-correo-ms:15000}")
  void enviarCorreos() {
    colaCorreo.procesarLote();
  }

  @Scheduled(
      initialDelayString = "${docentia.notificaciones.programador.espera-inicial-ms:30000}",
      fixedDelayString = "${docentia.notificaciones.programador.intervalo-recordatorios-ms:900000}")
  void generarRecordatorios() {
    recordatorios.generar();
  }

  @Scheduled(cron = "0 30 3 * * *", zone = "America/Bogota")
  void limpiarLeidas() {
    int borradas =
        notificacionRepository.borrarLeidasAntesDe(
            LocalDateTime.now(reloj).minusDays(propiedades.retencionDias()));
    LOG.info("Notificaciones leídas borradas por retención: {}", borradas);
  }
}
