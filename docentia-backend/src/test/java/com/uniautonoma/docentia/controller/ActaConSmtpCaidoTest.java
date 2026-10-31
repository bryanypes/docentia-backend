package com.uniautonoma.docentia.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.uniautonoma.docentia.service.notificacion.EnviadorCorreo;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Todo el flujo de actas debe funcionar igual con el servidor de correo caído. */
class ActaConSmtpCaidoTest extends ActaTest {

  @MockitoBean private EnviadorCorreo enviador;

  @BeforeEach
  void tumbarSmtp() {
    doThrow(new MailSendException("smtp caído")).when(enviador).enviar(any(), any(), any());
  }
}
