package com.uniautonoma.docentia.service.notificacion;

/** Apaga los avisos en el hilo actual. Solo para cargar datos de ejemplo. */
public final class SilencioNotificaciones {

  private static final ThreadLocal<Boolean> ACTIVO = ThreadLocal.withInitial(() -> false);

  private SilencioNotificaciones() {}

  public static void activar() {
    ACTIVO.set(true);
  }

  public static void desactivar() {
    ACTIVO.remove();
  }

  static boolean activo() {
    return ACTIVO.get();
  }
}
