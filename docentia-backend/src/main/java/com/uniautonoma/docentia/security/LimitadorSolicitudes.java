package com.uniautonoma.docentia.security;

import com.uniautonoma.docentia.exception.DemasiadosIntentosException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Ventana deslizante por clave, en memoria, como el límite del login. */
@Component
public class LimitadorSolicitudes {

  private static final int MAXIMO_CLAVES = 50_000;
  private static final Duration VENTANA = Duration.ofMinutes(1);

  private final Map<String, Deque<Instant>> solicitudes = new ConcurrentHashMap<>();
  private final Clock reloj;

  public LimitadorSolicitudes(Clock reloj) {
    this.reloj = reloj;
  }

  /**
   * @throws DemasiadosIntentosException si la clave superó el máximo en el último minuto
   */
  public void registrar(String clave, int maximoPorMinuto) {
    if (solicitudes.size() >= MAXIMO_CLAVES) {
      solicitudes.clear();
    }
    Deque<Instant> marcas = solicitudes.computeIfAbsent(clave, k -> new ArrayDeque<>());
    Instant ahora = reloj.instant();
    synchronized (marcas) {
      while (!marcas.isEmpty() && marcas.peekFirst().isBefore(ahora.minus(VENTANA))) {
        marcas.removeFirst();
      }
      if (marcas.size() >= maximoPorMinuto) {
        throw new DemasiadosIntentosException(
            "Demasiadas solicitudes. Espera un minuto y vuelve a intentarlo");
      }
      marcas.addLast(ahora);
    }
  }
}
