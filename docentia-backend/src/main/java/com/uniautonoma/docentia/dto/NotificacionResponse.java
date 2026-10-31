package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Notificacion;
import com.uniautonoma.docentia.entity.TipoNotificacion;
import java.time.LocalDateTime;

/** Un aviso de la bandeja. */
public record NotificacionResponse(
    Long id,
    TipoNotificacion tipo,
    String titulo,
    String mensaje,
    String enlace,
    boolean leida,
    LocalDateTime creadaEn) {

  public static NotificacionResponse desde(Notificacion notificacion) {
    return new NotificacionResponse(
        notificacion.getId(),
        notificacion.getTipo(),
        notificacion.getTitulo(),
        notificacion.getMensaje(),
        notificacion.getEnlace(),
        notificacion.isLeida(),
        notificacion.getCreadaEn());
  }
}
