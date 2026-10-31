package com.uniautonoma.docentia.service.notificacion;

import com.uniautonoma.docentia.config.NotificacionProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Envía la cola de correo. Tomar el lote deja cada fila apartada unos minutos con FOR UPDATE SKIP
 * LOCKED, así varias instancias no envían dos veces. El envío ocurre fuera de transacción. Las
 * fechas las pone la base, la misma que las generó al encolar.
 */
@Component
public class ProcesadorColaCorreo {

  static final int MAXIMO_INTENTOS = 5;
  private static final int MINUTOS_APARTADO = 5;
  private static final Logger LOG = LoggerFactory.getLogger(ProcesadorColaCorreo.class);

  private static final String TOMAR_LOTE =
      """
      update cola_correo
      set intentos = intentos + 1,
          proximo_intento = localtimestamp + (? * interval '1 minute')
      where id in (
          select id from cola_correo
          where estado = 'PENDIENTE' and proximo_intento <= localtimestamp
          order by proximo_intento, id
          for update skip locked
          limit ?)
      returning id, destinatario, asunto, cuerpo, intentos
      """;

  private final JdbcTemplate jdbcTemplate;
  private final EnviadorCorreo enviador;
  private final int lote;

  public ProcesadorColaCorreo(
      JdbcTemplate jdbcTemplate, EnviadorCorreo enviador, NotificacionProperties propiedades) {
    this.jdbcTemplate = jdbcTemplate;
    this.enviador = enviador;
    this.lote = propiedades.loteCorreo();
  }

  /**
   * Procesa un lote acotado.
   *
   * @return correos intentados
   */
  public int procesarLote() {
    List<CorreoTomado> tomados =
        jdbcTemplate.query(
            TOMAR_LOTE,
            (fila, numero) ->
                new CorreoTomado(
                    fila.getLong("id"),
                    fila.getString("destinatario"),
                    fila.getString("asunto"),
                    fila.getString("cuerpo"),
                    fila.getInt("intentos")),
            MINUTOS_APARTADO,
            lote);
    tomados.forEach(this::enviar);
    return tomados.size();
  }

  private void enviar(CorreoTomado correo) {
    try {
      enviador.enviar(correo.destinatario(), correo.asunto(), correo.cuerpo());
      jdbcTemplate.update(
          "update cola_correo set estado = 'ENVIADO', enviada_en = localtimestamp,"
              + " ultimo_error = null where id = ?",
          correo.id());
    } catch (RuntimeException fallo) {
      // Solo el tipo de error: el mensaje del servidor SMTP puede traer direcciones.
      String error = fallo.getClass().getSimpleName();
      LOG.warn("Falló el correo {} (intento {}): {}", correo.id(), correo.intentos(), error);
      if (correo.intentos() >= MAXIMO_INTENTOS) {
        jdbcTemplate.update(
            "update cola_correo set estado = 'ERROR', ultimo_error = ? where id = ?",
            error,
            correo.id());
      } else {
        jdbcTemplate.update(
            "update cola_correo set proximo_intento = localtimestamp"
                + " + (? * interval '1 minute'), ultimo_error = ? where id = ?",
            1 << correo.intentos(),
            error,
            correo.id());
      }
    }
  }

  private record CorreoTomado(
      long id, String destinatario, String asunto, String cuerpo, int intentos) {}
}
