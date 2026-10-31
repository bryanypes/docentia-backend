package com.uniautonoma.docentia.config;

import com.uniautonoma.docentia.service.notificacion.EnviadorCorreo;
import com.uniautonoma.docentia.service.notificacion.EnviadorCorreoRegistro;
import com.uniautonoma.docentia.service.notificacion.EnviadorCorreoSmtp;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Elige el enviador: SMTP si hay host; si no, solo log y un aviso al arrancar. */
@Configuration
@EnableConfigurationProperties(NotificacionProperties.class)
public class CorreoConfig {

  private static final Logger LOG = LoggerFactory.getLogger(CorreoConfig.class);

  /** Las pruebas invocan las tareas a mano. */
  @Configuration
  @EnableScheduling
  @ConditionalOnProperty(
      prefix = "docentia.notificaciones.programador",
      name = "habilitado",
      havingValue = "true",
      matchIfMissing = true)
  static class Programacion {}

  private static final String TIEMPO_MAXIMO_MS = String.valueOf(TimeUnit.SECONDS.toMillis(10));

  @Bean
  EnviadorCorreo enviadorCorreo(NotificacionProperties propiedades) {
    NotificacionProperties.Correo correo = propiedades.correo();
    if (!correo.configurado()) {
      LOG.warn("Sin DOCENTIA_MAIL_HOST: los correos solo se registran en el log");
      return new EnviadorCorreoRegistro();
    }
    JavaMailSenderImpl cliente = new JavaMailSenderImpl();
    cliente.setHost(correo.host());
    cliente.setPort(correo.puerto());
    boolean conUsuario = correo.usuario() != null && !correo.usuario().isBlank();
    if (conUsuario) {
      cliente.setUsername(correo.usuario());
      cliente.setPassword(correo.contrasena());
    }
    Properties opciones = cliente.getJavaMailProperties();
    opciones.put("mail.smtp.auth", String.valueOf(conUsuario));
    opciones.put("mail.smtp.starttls.enable", String.valueOf(correo.tls()));
    opciones.put("mail.smtp.starttls.required", String.valueOf(correo.tls()));
    opciones.put("mail.smtp.connectiontimeout", TIEMPO_MAXIMO_MS);
    opciones.put("mail.smtp.timeout", TIEMPO_MAXIMO_MS);
    opciones.put("mail.smtp.writetimeout", TIEMPO_MAXIMO_MS);
    return new EnviadorCorreoSmtp(cliente, correo.remitente());
  }
}
