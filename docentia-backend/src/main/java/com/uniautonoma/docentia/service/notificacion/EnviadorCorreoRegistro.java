package com.uniautonoma.docentia.service.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Para desarrollo y pruebas: registra que hubo un envío, sin el cuerpo ni el destinatario. */
public class EnviadorCorreoRegistro implements EnviadorCorreo {

  private static final Logger LOG = LoggerFactory.getLogger(EnviadorCorreoRegistro.class);

  @Override
  public void enviar(String destinatario, String asunto, String cuerpo) {
    LOG.info("Correo no enviado (sin SMTP configurado): {}", asunto);
  }
}
