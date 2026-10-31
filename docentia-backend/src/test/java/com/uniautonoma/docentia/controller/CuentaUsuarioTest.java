package com.uniautonoma.docentia.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.CambioContrasenaRequest;
import com.uniautonoma.docentia.dto.EstadoUsuarioRequest;
import com.uniautonoma.docentia.dto.InicioSesionRequest;
import com.uniautonoma.docentia.dto.UsuarioRequest;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Contraseña temporal obligatoria de cambiar y activación de cuentas. */
class CuentaUsuarioTest extends PruebaIntegracion {

  private static final String TEMPORAL = "Temporal-123";
  private static final String CORREO = "ana.ruiz@prueba.co";

  @Autowired private EscenarioPrueba escenario;

  private Facultad ingenieria;

  @BeforeEach
  void crearFacultad() {
    ingenieria = escenario.crearFacultad("Ingeniería");
  }

  @Test
  void unaCuentaNuevaQuedaConContrasenaTemporalYSoloPuedeCambiarla() throws Exception {
    String token = crearCoordinadorEIniciarSesion();

    mockMvc
        .perform(get("/sesiones/actual").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.debeCambiarContrasena").value(true));
    mockMvc
        .perform(get("/periodos-academicos").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.mensaje").value("Debes cambiar tu contraseña temporal antes de continuar"));
    mockMvc
        .perform(get("/docentes").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isForbidden());
  }

  @Test
  void cambiarLaContrasenaDevuelveUnaSesionSinLaMarca() throws Exception {
    String token = crearCoordinadorEIniciarSesion();

    String cuerpo =
        cambiarContrasena(token, TEMPORAL, "Clave-propia-2")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usuario.debeCambiarContrasena").value(false))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String tokenNuevo = objectMapper.readTree(cuerpo).get("token").asText();

    mockMvc
        .perform(get("/periodos-academicos").header(HttpHeaders.AUTHORIZATION, bearer(tokenNuevo)))
        .andExpect(status().isOk());
    iniciarSesion(CORREO, "Clave-propia-2")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.usuario.debeCambiarContrasena").value(false));
    iniciarSesion(CORREO, TEMPORAL).andExpect(status().isUnauthorized());
  }

  @Test
  void validaLaContrasenaActualYQueLaNuevaSeaDistinta() throws Exception {
    String token = crearCoordinadorEIniciarSesion();

    cambiarContrasena(token, "no-es-la-actual", "Clave-propia-2")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.contrasenaActual").exists());
    cambiarContrasena(token, TEMPORAL, TEMPORAL)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.contrasenaNueva").exists());
    cambiarContrasena(token, TEMPORAL, "corta")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.contrasenaNueva").exists());
  }

  @Test
  void cambiarLaContrasenaExigeSesion() throws Exception {
    mockMvc
        .perform(
            put("/sesiones/actual/contrasena")
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new CambioContrasenaRequest(TEMPORAL, "Clave-propia-2"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void restablecerLaContrasenaLaVuelveTemporal() throws Exception {
    Usuario docente =
        escenario.crearDocente("Julian Hoyos", escenario.crearPrograma("Software", ingenieria));

    mockMvc
        .perform(
            put("/usuarios/{id}", docente.getIdUsuario())
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        new UsuarioRequest(
                            "Julian Hoyos",
                            docente.getCorreo(),
                            Rol.DOCENTE,
                            null,
                            docente.getPrograma().getIdPrograma(),
                            TEMPORAL))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.debeCambiarContrasena").value(true));
  }

  @Test
  void laVicerrectoriaDesactivaYReactivaUnaCuenta() throws Exception {
    crearCoordinador();
    Long id = idDe(CORREO);

    cambiarEstado(id, false)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.esActivo").value(false))
        .andExpect(jsonPath("$.nombreCompleto").value("Ana Ruiz"));
    iniciarSesion(CORREO, TEMPORAL).andExpect(status().isUnauthorized());

    cambiarEstado(id, true).andExpect(status().isOk());
    iniciarSesion(CORREO, TEMPORAL).andExpect(status().isCreated());
  }

  @Test
  void nadieDesactivaSuPropiaCuentaYSoloLaVicerrectoriaCambiaElEstado() throws Exception {
    Usuario vicerrectora = escenario.crearVicerrector("Elena Munoz");
    Usuario coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);

    mockMvc
        .perform(
            patch("/usuarios/{id}/estado", vicerrectora.getIdUsuario())
                .with(como(vicerrectora))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new EstadoUsuarioRequest(false))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("No puedes desactivar tu propia cuenta"));
    mockMvc
        .perform(
            patch("/usuarios/{id}/estado", vicerrectora.getIdUsuario())
                .with(como(coordinador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new EstadoUsuarioRequest(false))))
        .andExpect(status().isForbidden());
    cambiarEstado(999_999L, false).andExpect(status().isNotFound());
    mockMvc
        .perform(
            patch("/usuarios/{id}/estado", coordinador.getIdUsuario())
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.esActivo").exists());
  }

  private void crearCoordinador() throws Exception {
    mockMvc
        .perform(
            post("/usuarios")
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        new UsuarioRequest(
                            "Ana Ruiz",
                            CORREO,
                            Rol.COORDINADOR,
                            ingenieria.getIdFacultad(),
                            null,
                            TEMPORAL))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.debeCambiarContrasena").value(true));
  }

  private String crearCoordinadorEIniciarSesion() throws Exception {
    crearCoordinador();
    String cuerpo =
        iniciarSesion(CORREO, TEMPORAL)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.usuario.debeCambiarContrasena").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(cuerpo).get("token").asText();
  }

  private Long idDe(String correo) throws Exception {
    String cuerpo =
        mockMvc
            .perform(get("/usuarios").with(comoVicerrector()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    for (var usuario : objectMapper.readTree(cuerpo)) {
      if (correo.equals(usuario.get("correo").asText())) {
        return usuario.get("idUsuario").asLong();
      }
    }
    throw new AssertionError("No se encontró " + correo);
  }

  private ResultActions iniciarSesion(String correo, String contrasena) throws Exception {
    return mockMvc.perform(
        post("/sesiones")
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new InicioSesionRequest(correo, contrasena))));
  }

  private ResultActions cambiarContrasena(String token, String actual, String nueva)
      throws Exception {
    return mockMvc.perform(
        put("/sesiones/actual/contrasena")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new CambioContrasenaRequest(actual, nueva))));
  }

  private ResultActions cambiarEstado(Long idUsuario, boolean esActivo) throws Exception {
    return mockMvc.perform(
        patch("/usuarios/{id}/estado", idUsuario)
            .with(comoVicerrector())
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new EstadoUsuarioRequest(esActivo))));
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }
}
