package com.uniautonoma.docentia.service.notificacion;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** Envío real por SMTP. */
public class EnviadorCorreoSmtp implements EnviadorCorreo {

  private final JavaMailSender cliente;
  private final String remitente;

  public EnviadorCorreoSmtp(JavaMailSender cliente, String remitente) {
    this.cliente = cliente;
    this.remitente = remitente;
  }

  @Override
  public void enviar(String destinatario, String asunto, String cuerpo) {
    SimpleMailMessage correo = new SimpleMailMessage();
    correo.setFrom(remitente);
    correo.setTo(destinatario);
    correo.setSubject(asunto);
    correo.setText(cuerpo);
    cliente.send(correo);
  }
}
