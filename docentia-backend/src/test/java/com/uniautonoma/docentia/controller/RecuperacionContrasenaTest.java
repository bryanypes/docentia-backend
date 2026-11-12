package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Usuario;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

/** Recuperar la contraseña: respuesta genérica, token de un uso con hash, vencimiento y límite. */
@ExtendWith(OutputCaptureExtension.class)
class RecuperacionContrasenaTest extends PruebaIntegracion {

  private static final AtomicInteger IP = new AtomicInteger(1);
  private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");
  private static final String NUEVA = "Nueva-Clave-2026";

  @Autowired private EscenarioPrueba escenario;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManager entityManager;

  private Usuario coordinador;

  @BeforeEach
  void crearEscenario() {
    Facultad facultad = escenario.crearFacultad("Ingeniería");
    coordinador = escenario.crearCoordinador("Coordinadora Recupera", facultad);
  }

  /** Cada prueba usa su propia IP para no chocar con el límite de las demás. */
  private String ip() {
    return "10.0.0." + IP.getAndIncrement();
  }

  private ResultActions solicitar(String correo, String ip) throws Exception {
    return mockMvc.perform(
        post("/sesiones/recuperacion")
            .header("X-Forwarded-For", ip)
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(Map.of("correo", correo))));
  }

  private ResultActions restablecer(String token, String clave) throws Exception {
    return mockMvc.perform(
        post("/sesiones/recuperacion/restablecimiento")
            .header("X-Forwarded-For", ip())
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(Map.of("token", token, "contrasena", clave))));
  }

  private String tokenDelCorreo() {
    String cuerpo =
        jdbcTemplate.queryForObject(
            "select cuerpo from cola_correo where destinatario = ? order by id desc limit 1",
            String.class,
            coordinador.getCorreo());
    Matcher m = TOKEN.matcher(cuerpo);
    assertThat(m.find()).isTrue();
    return m.group(1);
  }

  @Test
  void respondeIgualExistaONoElCorreoYSoloEncolaParaElQueExiste() throws Exception {
    String ip = ip();
    String existe =
        solicitar(coordinador.getCorreo(), ip)
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String noExiste =
        solicitar("nadie@uniautonoma.edu.co", ip)
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(noExiste).isEqualTo(existe);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from cola_correo where asunto = 'DOCENTIA: Restablecer tu contraseña'",
                Long.class))
        .isEqualTo(1L);
    String token = tokenDelCorreo();
    String guardado =
        jdbcTemplate.queryForObject(
            "select hash_token from recuperaciones_contrasena", String.class);
    assertThat(guardado).isNotEqualTo(token).hasSize(64);
  }

  @Test
  void elTokenCambiaLaClaveUnaSolaVez(CapturedOutput salida) throws Exception {
    solicitar(coordinador.getCorreo(), ip()).andExpect(status().isAccepted());
    String token = tokenDelCorreo();

    restablecer(token, "corta").andExpect(status().isBadRequest());
    restablecer(token, NUEVA).andExpect(status().isNoContent());
    // El cambio se hizo por SQL; en producción el login es otra transacción.
    entityManager.clear();
    mockMvc
        .perform(
            post("/sesiones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("correo", coordinador.getCorreo(), "contrasena", NUEVA))))
        .andExpect(status().isCreated());
    restablecer(token, "Otra-Clave-2026")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.token").exists());

    assertThat(salida.getAll()).doesNotContain(token).doesNotContain(NUEVA);
  }

  @Test
  void unTokenVencidoNoSirve() throws Exception {
    solicitar(coordinador.getCorreo(), ip()).andExpect(status().isAccepted());
    String token = tokenDelCorreo();
    jdbcTemplate.update(
        "update recuperaciones_contrasena set vence_en = vence_en - interval '31 minutes'");

    restablecer(token, NUEVA).andExpect(status().isBadRequest());
    restablecer("token-inventado", NUEVA).andExpect(status().isBadRequest());
  }

  @Test
  void demasiadasSolicitudesDesdeLaMismaIpResponden429() throws Exception {
    String ip = ip();
    for (int i = 0; i < 5; i += 1) {
      solicitar("persona" + i + "@uniautonoma.edu.co", ip).andExpect(status().isAccepted());
    }
    solicitar("persona9@uniautonoma.edu.co", ip).andExpect(status().isTooManyRequests());
  }
}
