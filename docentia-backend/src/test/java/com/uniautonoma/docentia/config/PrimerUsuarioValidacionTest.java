package com.uniautonoma.docentia.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** La configuración se valida antes de tocar la base; un error detiene el arranque. */
class PrimerUsuarioValidacionTest {

  private static final GoogleProperties SIN_GOOGLE = new GoogleProperties("");
  private static final GoogleProperties CON_GOOGLE =
      new GoogleProperties("cliente.apps.googleusercontent.com");
  private static final String CORREO = "jefe@uniautonoma.edu.co";

  private static PrimerUsuario con(
      GoogleProperties google, String correo, String nombre, String contrasena) {
    return new PrimerUsuario(null, null, null, null, google, correo, nombre, contrasena);
  }

  @Test
  void sinCorreoNoHaceNada() {
    assertThat(con(SIN_GOOGLE, "", "", "").crearSiHaceFalta()).isFalse();
    assertThat(con(SIN_GOOGLE, null, null, null).crearSiHaceFalta()).isFalse();
  }

  @Test
  void exigeCorreoInstitucionalYNombre() {
    assertThatThrownBy(() -> con(SIN_GOOGLE, "jefe@gmail.com", "Jefe", "clave-larga-123").validar())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("@uniautonoma.edu.co");
    assertThatThrownBy(() -> con(SIN_GOOGLE, CORREO, " ", "clave-larga-123").validar())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DOCENTIA_BOOTSTRAP_NOMBRE");
  }

  @Test
  void laContrasenaTieneEntre12Y72CaracteresYNoApareceEnElError() {
    assertThatThrownBy(() -> con(SIN_GOOGLE, CORREO, "Jefe", "corta-11ch").validar())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("entre 12 y 72")
        .hasMessageNotContaining("corta-11ch");
    con(SIN_GOOGLE, CORREO, "Jefe", "x".repeat(12)).validar();
    assertThatThrownBy(() -> con(SIN_GOOGLE, CORREO, "Jefe", "x".repeat(73)).validar())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void sinContrasenaHaceFaltaGoogle() {
    assertThatThrownBy(() -> con(SIN_GOOGLE, CORREO, "Jefe", "").validar())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DOCENTIA_GOOGLE_CLIENT_ID");
    con(CON_GOOGLE, CORREO, "Jefe", "").validar();
  }
}
