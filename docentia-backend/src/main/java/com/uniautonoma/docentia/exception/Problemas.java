package com.uniautonoma.docentia.exception;

import com.uniautonoma.docentia.config.FiltroCorrelacion;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * Errores en formato RFC 7807. Se conservan {@code mensaje} y {@code errores} como campos
 * adicionales, que son los que lee el frontend.
 */
public final class Problemas {

  private Problemas() {}

  public static ProblemDetail crear(HttpStatusCode estado, Map<String, Object> campos) {
    ProblemDetail problema = ProblemDetail.forStatus(estado);
    HttpStatus conocido = HttpStatus.resolve(estado.value());
    problema.setTitle(conocido == null ? "Error" : conocido.getReasonPhrase());
    Object mensaje = campos.get("mensaje");
    problema.setDetail(mensaje == null ? null : mensaje.toString());
    campos.forEach(problema::setProperty);
    problema.setProperty("correlationId", MDC.get(FiltroCorrelacion.CLAVE_MDC));
    return problema;
  }
}
