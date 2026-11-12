package com.uniautonoma.docentia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.config.JwtProperties;
import com.uniautonoma.docentia.dto.InicioSesionRequest;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.security.ProveedorTokenJwt;
import com.uniautonoma.docentia.security.UsuarioSesion;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

/** Token, cabeceras, errores uniformes y límite de inicio de sesión. */
@TestPropertySource(properties = {"docentia.login.maximo-por-correo=3"})
class SeguridadHttpTest extends PruebaIntegracion {

  private static final String SECRETO = "secreto-exclusivo-para-las-pruebas-automatizadas";
  private static final UsuarioSesion SESION =
      new UsuarioSesion(1L, "Ana", "ana@prueba.co", Rol.VICERRECTOR, null, null, false);

  @Autowired private ProveedorTokenJwt proveedor;
  @Autowired private EscenarioPrueba escenario;

  private org.springframework.test.web.servlet.ResultActions conToken(String token)
      throws Exception {
    return mockMvc.perform(
        get("/sesiones/actual").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
  }

  @Test
  void aceptaUnTokenValido() throws Exception {
    conToken(proveedor.generarToken(SESION)).andExpect(status().isOk());
  }

  @Test
  void rechazaTokenManipulado() throws Exception {
    String token = proveedor.generarToken(SESION);
    String[] partes = token.split("\\.");
    String carga =
        new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
            .replace("VICERRECTOR", "COORDINADOR");
    String otra =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(carga.getBytes(StandardCharsets.UTF_8));
    conToken(partes[0] + "." + otra + "." + partes[2]).andExpect(status().isUnauthorized());
  }

  @Test
  void rechazaTokenVencidoYAlgoritmosDistintos() throws Exception {
    var llave = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));
    conToken(
            Jwts.builder()
                .subject("ana@prueba.co")
                .claim("idUsuario", 1)
                .claim("rol", "VICERRECTOR")
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(llave, Jwts.SIG.HS256)
                .compact())
        .andExpect(status().isUnauthorized());
    conToken(
            Jwts.builder()
                .subject("ana@prueba.co")
                .claim("idUsuario", 1)
                .claim("rol", "VICERRECTOR")
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(llave, Jwts.SIG.HS384)
                .compact())
        .andExpect(status().isUnauthorized());
    String sinFirma =
        Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes())
            + "."
            + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                    "{\"sub\":\"a\",\"idUsuario\":1,\"rol\":\"VICERRECTOR\"}".getBytes())
            + ".";
    conToken(sinFirma).andExpect(status().isUnauthorized());
  }

  @Test
  void unSecretoCortoImpideArrancar() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new ProveedorTokenJwt(new JwtProperties("corto", 60)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void losErroresSonProblemDetailConCorrelacion() throws Exception {
    mockMvc
        .perform(get("/docentes").with(comoVicerrector()).param("facultad", "x"))
        .andExpect(header().exists("X-Correlation-Id"));
    mockMvc
        .perform(get("/facultades/987654").with(comoVicerrector()))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").exists())
        .andExpect(jsonPath("$.detail").exists())
        .andExpect(jsonPath("$.mensaje").exists())
        .andExpect(jsonPath("$.correlationId").exists());
    mockMvc
        .perform(get("/docentes"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.correlationId").exists())
        .andExpect(content().string(not(containsString("Exception"))));
  }

  @Test
  void cabecerasDeSeguridad() throws Exception {
    mockMvc
        .perform(get("/docentes"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("Referrer-Policy", "no-referrer"))
        .andExpect(header().string("Content-Security-Policy", "default-src 'none'"));
  }

  @Test
  void soloSaludEInfoQuedanExpuestos() throws Exception {
    mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    // La versión publicada se confirma sin sesión, para la prueba de humo del despliegue.
    mockMvc
        .perform(get("/actuator/info"))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.build.version")
                .exists());
    // Sin comodines: un subcomponente de la salud no queda público.
    mockMvc.perform(get("/actuator/health/db")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/actuator/env").with(comoVicerrector())).andExpect(status().isNotFound());
  }

  @Test
  void bloqueaTrasVariosFallosSinRevelarSiElUsuarioExiste() throws Exception {
    for (int i = 0; i < 3; i++) {
      intentar("nadie-existe@uniautonoma.edu.co", "x").andExpect(status().isUnauthorized());
    }
    String mensajeNoExiste =
        intentar("nadie-existe@uniautonoma.edu.co", "x")
            .andExpect(status().isTooManyRequests())
            .andReturn()
            .getResponse()
            .getContentAsString();
    org.assertj.core.api.Assertions.assertThat(mensajeNoExiste).contains("Demasiados intentos");

    Usuario real = escenario.crearVicerrector("Real Limite");
    for (int i = 0; i < 3; i++) {
      intentar(real.getCorreo(), "mala").andExpect(status().isUnauthorized());
    }
    intentar(real.getCorreo(), "mala").andExpect(status().isTooManyRequests());
  }

  private org.springframework.test.web.servlet.ResultActions intentar(String correo, String clave)
      throws Exception {
    return mockMvc.perform(
        post("/sesiones")
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new InicioSesionRequest(correo, clave))));
  }
}
