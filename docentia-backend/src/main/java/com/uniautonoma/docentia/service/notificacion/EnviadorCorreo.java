package com.uniautonoma.docentia.service.notificacion;

/** Entrega un correo de texto plano. Si falla, lanza una excepción y la cola lo reintenta. */
public interface EnviadorCorreo {

  void enviar(String destinatario, String asunto, String cuerpo);
}
