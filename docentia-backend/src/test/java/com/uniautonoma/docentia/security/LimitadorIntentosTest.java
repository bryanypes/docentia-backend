package com.uniautonoma.docentia.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.uniautonoma.docentia.exception.DemasiadosIntentosException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LimitadorIntentosTest {

  private static final String CORREO = "ana@uniautonoma.edu.co";
  private static final String IP = "10.0.0.1";

  private final RelojMovil reloj = new RelojMovil(Instant.parse("2026-11-13T15:00:00Z"));
  private final LimitadorIntentos limitador = new LimitadorIntentos(reloj, 10, 3, 30);

  @Test
  void cadaFalloDevuelveCuantosLlevaElCorreo() {
    assertThat(limitador.registrarFallo(CORREO, IP)).isEqualTo(1);
    assertThat(limitador.registrarFallo(" ANA@uniautonoma.edu.co ", IP)).isEqualTo(2);
    assertThat(limitador.getMaximoPorCorreo()).isEqualTo(3);
  }

  @Test
  void elBloqueoDiceCuantoFaltaYSeLiberaAlVencerElFalloDecisivo() {
    limitador.registrarFallo(CORREO, IP);
    reloj.avanzar(Duration.ofMinutes(2));
    limitador.registrarFallo(CORREO, IP);
    limitador.registrarFallo(CORREO, IP);

    assertThat(esperaDe()).isEqualTo(Duration.ofMinutes(8).toSeconds());

    reloj.avanzar(Duration.ofMinutes(5));
    assertThat(esperaDe()).isEqualTo(Duration.ofMinutes(3).toSeconds());

    // A los 10 minutos justos el primer fallo aún cuenta; un segundo después ya venció.
    reloj.avanzar(Duration.ofMinutes(3));
    assertThat(esperaDe()).isEqualTo(1);
    reloj.avanzar(Duration.ofSeconds(1));
    assertThatCode(() -> limitador.exigirPermitido(CORREO, IP)).doesNotThrowAnyException();
  }

  @Test
  void elMensajeDelBloqueoDiceLosMinutos() {
    for (int i = 0; i < 3; i++) {
      limitador.registrarFallo(CORREO, IP);
    }
    DemasiadosIntentosException bloqueo =
        catchThrowableOfType(
            DemasiadosIntentosException.class, () -> limitador.exigirPermitido(CORREO, IP));
    assertThat(bloqueo.getMessage())
        .isEqualTo("Demasiados intentos. Podrás volver a intentarlo en 10 minutos");
  }

  private long esperaDe() {
    return catchThrowableOfType(
            DemasiadosIntentosException.class, () -> limitador.exigirPermitido(CORREO, IP))
        .getSegundosParaReintentar();
  }

  private static final class RelojMovil extends Clock {

    private Instant ahora;

    RelojMovil(Instant inicio) {
      this.ahora = inicio;
    }

    void avanzar(Duration tiempo) {
      ahora = ahora.plus(tiempo);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
      return this;
    }

    @Override
    public Instant instant() {
      return ahora;
    }
  }
}
