package com.uniautonoma.docentia.service.notificacion;

import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.TipoNotificacion;
import com.uniautonoma.docentia.entity.Usuario;

/**
 * Algo que debe avisarse cuando la transacción de negocio se confirme.
 *
 * @param idDestino usuario o facultad, según {@code destino}
 * @param enlace ruta interna de la app
 * @param claveDedupe identifica el suceso: repetirlo no crea otro aviso
 * @param detalle texto corto opcional, como un código de verificación; nunca notas
 */
public record EventoNotificacion(
    TipoNotificacion tipo,
    Destino destino,
    Long idDestino,
    String enlace,
    String entidadTipo,
    Long entidadId,
    String claveDedupe,
    String detalle) {

  /** A quién se dirige: el alcance se resuelve aquí, no en quien publica. */
  public enum Destino {
    USUARIO,
    COORDINADORES_FACULTAD,
    VICERRECTORES,
    ADMINISTRADORES
  }

  public static EventoNotificacion aUsuario(
      TipoNotificacion tipo, Long idUsuario, String enlace, String entidad, Long id, String clave) {
    return new EventoNotificacion(
        tipo, Destino.USUARIO, idUsuario, enlace, entidad, id, clave, null);
  }

  /**
   * Avisa a quien evalúa al docente: la coordinación de su facultad o, si el evaluado es un
   * coordinador, la vicerrectoría.
   */
  public static EventoNotificacion aEvaluadores(
      TipoNotificacion tipo,
      Usuario docente,
      String enlace,
      String entidad,
      Long id,
      String clave) {
    if (docente.getRol() == Rol.COORDINADOR) {
      return aVicerrectores(tipo, enlace, entidad, id, clave);
    }
    return new EventoNotificacion(
        tipo,
        Destino.COORDINADORES_FACULTAD,
        docente.getFacultadDeAdscripcion().getIdFacultad(),
        enlace,
        entidad,
        id,
        clave,
        null);
  }

  public static EventoNotificacion aVicerrectores(
      TipoNotificacion tipo, String enlace, String entidad, Long id, String clave) {
    return new EventoNotificacion(
        tipo, Destino.VICERRECTORES, null, enlace, entidad, id, clave, null);
  }

  public static EventoNotificacion aAdministradores(
      TipoNotificacion tipo, String enlace, String entidad, Long id, String clave) {
    return new EventoNotificacion(
        tipo, Destino.ADMINISTRADORES, null, enlace, entidad, id, clave, null);
  }

  public EventoNotificacion conDetalle(String texto) {
    return new EventoNotificacion(
        tipo, destino, idDestino, enlace, entidadTipo, entidadId, claveDedupe, texto);
  }
}
