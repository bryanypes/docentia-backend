package com.uniautonoma.docentia.security;

import com.uniautonoma.docentia.exception.DemasiadosIntentosException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ventana deslizante de intentos fallidos por correo y por IP. Vive en memoria: con varias
 * instancias cada una cuenta aparte; en un despliegue real iría en Redis. Cuenta igual correos que
 * no existen, así la respuesta no revela cuáles sí.
 */
@Component
public class LimitadorIntentos {

  private static final int MAXIMO_CLAVES = 50_000;

  private final Map<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();
  private final Clock reloj;
  private final Duration ventana;
  private final int maximoPorCorreo;
  private final int maximoPorIp;

  public LimitadorIntentos(
      Clock reloj,
      @Value("${docentia.login.ventana-minutos:10}") long minutos,
      @Value("${docentia.login.maximo-por-correo:5}") int maximoPorCorreo,
      @Value("${docentia.login.maximo-por-ip:30}") int maximoPorIp) {
    this.reloj = reloj;
    this.ventana = Duration.ofMinutes(minutos);
    this.maximoPorCorreo = maximoPorCorreo;
    this.maximoPorIp = maximoPorIp;
  }

  /**
   * @throws DemasiadosIntentosException si el correo o la IP superaron el límite, con los segundos
   *     que faltan para volver a intentar
   */
  public void exigirPermitido(String correo, String ip) {
    long espera =
        Math.max(
            segundosHastaLiberar(claveCorreo(correo), maximoPorCorreo),
            segundosHastaLiberar("ip:" + ip, maximoPorIp));
    if (espera > 0) {
      throw new DemasiadosIntentosException(espera);
    }
  }

  /**
   * @return intentos fallidos del correo dentro de la ventana, contando este
   */
  public int registrarFallo(String correo, String ip) {
    if (fallos.size() >= MAXIMO_CLAVES) {
      fallos.keySet().removeIf(clave -> contar(clave) == 0);
    }
    agregar(claveCorreo(correo));
    agregar("ip:" + ip);
    return contar(claveCorreo(correo));
  }

  public int getMaximoPorCorreo() {
    return maximoPorCorreo;
  }

  public void registrarExito(String correo) {
    fallos.remove(claveCorreo(correo));
  }

  private void agregar(String clave) {
    Deque<Instant> intentos = fallos.computeIfAbsent(clave, k -> new ArrayDeque<>());
    synchronized (intentos) {
      intentos.addLast(reloj.instant());
    }
  }

  private int contar(String clave) {
    Deque<Instant> intentos = fallos.get(clave);
    if (intentos == null) {
      return 0;
    }
    Instant limite = reloj.instant().minus(ventana);
    synchronized (intentos) {
      while (!intentos.isEmpty() && intentos.peekFirst().isBefore(limite)) {
        intentos.removeFirst();
      }
      return intentos.size();
    }
  }

  /**
   * Segundos hasta que la clave baje del máximo: vence el fallo que la mantiene en el límite. Cero
   * si no está bloqueada.
   */
  private long segundosHastaLiberar(String clave, int maximo) {
    if (contar(clave) < maximo) {
      return 0;
    }
    Deque<Instant> intentos = fallos.get(clave);
    synchronized (intentos) {
      Instant decisivo = intentos.stream().skip(intentos.size() - maximo).findFirst().orElseThrow();
      long milis = Duration.between(reloj.instant(), decisivo.plus(ventana)).toMillis();
      return Math.max(1, (milis + 999) / 1000);
    }
  }

  private static String claveCorreo(String correo) {
    return "correo:" + correo.trim().toLowerCase(Locale.ROOT);
  }
}
