package com.uniautonoma.docentia.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Arranque real con las variables DOCENTIA_BOOTSTRAP_* sobre una base nueva (este contexto tiene su
 * propio PostgreSQL embebido): se crea un solo vicerrector, con contraseña temporal, y nada
 * sensible va al log.
 */
@SpringBootTest(
    properties = {
      "docentia.bootstrap.email=" + PrimerUsuarioTest.CORREO_ENTORNO,
      "docentia.bootstrap.nombre=Vicerrectoría Inicial",
      "docentia.bootstrap.contrasena=" + PrimerUsuarioTest.CLAVE
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BaseDatosPruebaConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class PrimerUsuarioTest {

  static final String CORREO_ENTORNO = "Vicerrectoria.Inicial@UniAutonoma.edu.co";
  static final String CORREO = "vicerrectoria.inicial@uniautonoma.edu.co";
  static final String CLAVE = "Clave-Inicial-De-Prueba-2026";

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private PrimerUsuario primerUsuario;
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void alArrancarCreaUnSoloVicerrectorConContrasenaTemporal() {
    assertThat(jdbcTemplate.queryForObject("select count(*) from usuarios", Long.class))
        .isEqualTo(1L);
    Map<String, Object> fila =
        jdbcTemplate.queryForMap(
            "select correo, nombre_completo, rol, debe_cambiar_contrasena, contrasena_hash"
                + " from usuarios");
    assertThat(fila.get("correo")).isEqualTo(CORREO);
    assertThat(fila.get("nombre_completo")).isEqualTo("Vicerrectoría Inicial");
    assertThat(fila.get("rol")).isEqualTo("VICERRECTOR");
    assertThat(fila.get("debe_cambiar_contrasena")).isEqualTo(true);
    assertThat(passwordEncoder.matches(CLAVE, (String) fila.get("contrasena_hash"))).isTrue();
  }

  @Test
  void otraCorridaNoCreaNadaYNoRegistraLaClaveNiElCorreo(CapturedOutput salida) {
    assertThat(primerUsuario.crearSiHaceFalta()).isFalse();

    assertThat(jdbcTemplate.queryForObject("select count(*) from usuarios", Long.class))
        .isEqualTo(1L);
    assertThat(salida.getAll())
        .contains("ya hay usuarios")
        .doesNotContain(CLAVE)
        .doesNotContain(CORREO);
  }

  @Test
  void entraConLaClaveInicialYSoloPuedeCambiarla() throws Exception {
    String cuerpo =
        mockMvc
            .perform(
                post("/sesiones")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of("correo", CORREO, "contrasena", CLAVE))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = objectMapper.readTree(cuerpo).get("token").asText();

    mockMvc
        .perform(get("/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/sesiones/actual").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk());
  }
}
