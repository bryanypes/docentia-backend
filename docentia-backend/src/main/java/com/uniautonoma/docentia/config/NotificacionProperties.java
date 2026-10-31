package com.uniautonoma.docentia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Notificaciones y correo. Sin {@code host} los correos solo se registran en el log.
 *
 * @param urlAplicacion dirección pública de la app, para los enlaces de los correos
 * @param retencionDias días que se conserva un aviso ya leído
 * @param loteCorreo correos que toma cada pasada del procesador
 */
@ConfigurationProperties(prefix = "docentia.notificaciones")
public record NotificacionProperties(
    @DefaultValue("http://localhost:5173") String urlAplicacion,
    @DefaultValue("90") int retencionDias,
    @DefaultValue("20") int loteCorreo,
    @DefaultValue Correo correo) {

  public record Correo(
      String host,
      @DefaultValue("587") int puerto,
      String usuario,
      String contrasena,
      @DefaultValue("no-responder@uniautonoma.edu.co") String remitente,
      @DefaultValue("true") boolean tls) {

    public boolean configurado() {
      return host != null && !host.isBlank();
    }
  }
}
