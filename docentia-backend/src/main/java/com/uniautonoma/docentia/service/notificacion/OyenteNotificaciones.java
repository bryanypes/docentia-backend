package com.uniautonoma.docentia.service.notificacion;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Atiende los eventos solo si el negocio se confirmó: una reversión no avisa nada. */
@Component
class OyenteNotificaciones {

  private final NotificacionService notificacionService;

  OyenteNotificaciones(NotificacionService notificacionService) {
    this.notificacionService = notificacionService;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void alConfirmar(EventoNotificacion evento) {
    notificacionService.notificar(evento);
  }
}
