package com.uniautonoma.docentia.exception;

/** Límite de intentos de inicio de sesión. El mensaje no dice si el usuario existe. */
public class DemasiadosIntentosException extends RuntimeException {

  /** Segundos que faltan para volver a intentar; cero si no se conocen. */
  private final long segundosParaReintentar;

  public DemasiadosIntentosException() {
    super("Demasiados intentos. Espera unos minutos y vuelve a intentarlo");
    this.segundosParaReintentar = 0;
  }

  public DemasiadosIntentosException(String mensaje) {
    super(mensaje);
    this.segundosParaReintentar = 0;
  }

  public DemasiadosIntentosException(long segundosParaReintentar) {
    super(
        "Demasiados intentos. Podrás volver a intentarlo en "
            + describirEspera(segundosParaReintentar));
    this.segundosParaReintentar = segundosParaReintentar;
  }

  public long getSegundosParaReintentar() {
    return segundosParaReintentar;
  }

  private static String describirEspera(long segundos) {
    long minutos = (segundos + 59) / 60;
    return minutos == 1 ? "1 minuto" : minutos + " minutos";
  }
}
