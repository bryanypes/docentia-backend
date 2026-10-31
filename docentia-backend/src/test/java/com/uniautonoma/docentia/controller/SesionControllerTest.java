package com.uniautonoma.docentia.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.FacultadRequest;
import com.uniautonoma.docentia.dto.InicioSesionRequest;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.FacultadRepository;
import com.uniautonoma.docentia.repository.ProgramaRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

class SesionControllerTest extends PruebaIntegracion {

  private static final String CONTRASENA = "Clave-Segura-123";
  private static final String CORREO_COORDINADOR = "marta.ruiz@uniautonoma.edu.co";

  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private FacultadRepository facultadRepository;
  @Autowired private ProgramaRepository programaRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private Facultad ingenieria;
  private Usuario coordinador;

  @BeforeEach
  void crearUsuarios() {
    ingenieria = facultadRepository.save(new Facultad("Ingeniería"));
    coordinador =
        usuarioRepository.save(
            Usuario.crearCoordinador(
                "Marta Ruiz", CORREO_COORDINADOR, passwordEncoder.encode(CONTRASENA), ingenieria));
  }

  @Test
  void elLoginDevuelveElTokenConElAlcanceDelCoordinador() throws Exception {
    iniciarSesion(CORREO_COORDINADOR, CONTRASENA)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.usuario.rol").value("COORDINADOR"))
        .andExpect(jsonPath("$.usuario.idFacultad").value(ingenieria.getIdFacultad()))
        .andExpect(jsonPath("$.usuario.contrasenaHash").doesNotExist());
  }

  @Test
  void elLoginDelDocenteIncluyeSuProgramaYLaFacultadDelPrograma() throws Exception {
    Programa software = programaRepository.save(new Programa("Ingeniería de Software", ingenieria));
    usuarioRepository.save(
        Usuario.crearDocente(
            "Luis Paz",
            "luis.paz@uniautonoma.edu.co",
            passwordEncoder.encode(CONTRASENA),
            software));

    iniciarSesion("luis.paz@uniautonoma.edu.co", CONTRASENA)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.usuario.idPrograma").value(software.getIdPrograma()))
        .andExpect(jsonPath("$.usuario.idFacultad").value(ingenieria.getIdFacultad()));
  }

  @Test
  void unaContrasenaIncorrectaYUnCorreoDesconocidoRespondenIgual() throws Exception {
    String mensaje = "Correo o contraseña incorrectos";

    iniciarSesion(CORREO_COORDINADOR, "otra-clave")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.mensaje").value(mensaje));
    iniciarSesion("nadie@uniautonoma.edu.co", CONTRASENA)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.mensaje").value(mensaje));
  }

  @Test
  void unUsuarioInactivoNoPuedeIniciarSesion() throws Exception {
    coordinador.setEsActivo(false);
    usuarioRepository.saveAndFlush(coordinador);

    iniciarSesion(CORREO_COORDINADOR, CONTRASENA).andExpect(status().isUnauthorized());
  }

  @Test
  void sinTokenLasRutasProtegidasRespondenNoAutenticado() throws Exception {
    mockMvc
        .perform(get("/facultades"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.mensaje").value("Debes iniciar sesión para continuar"));
  }

  @Test
  void unTokenConLaFirmaAlteradaNoAutentica() throws Exception {
    String[] partes = obtenerToken(CORREO_COORDINADOR).split("\\.");
    String firmaAlterada = new StringBuilder(partes[2]).reverse().toString();
    String token = partes[0] + "." + partes[1] + "." + firmaAlterada;

    mockMvc
        .perform(get("/sesiones/actual").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void conUnTokenValidoSeRecuperaLaSesion() throws Exception {
    String token = obtenerToken(CORREO_COORDINADOR);

    mockMvc
        .perform(get("/sesiones/actual").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.correo").value(CORREO_COORDINADOR))
        .andExpect(jsonPath("$.idFacultad").value(ingenieria.getIdFacultad()));
  }

  @Test
  void elCoordinadorNoPuedeCrearFacultades() throws Exception {
    String token = obtenerToken(CORREO_COORDINADOR);

    mockMvc
        .perform(
            post("/facultades")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new FacultadRequest("Derecho"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.mensaje").value("No tienes permiso para realizar esta acción"));
  }

  private ResultActions iniciarSesion(String correo, String contrasena) throws Exception {
    return mockMvc.perform(
        post("/sesiones")
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new InicioSesionRequest(correo, contrasena))));
  }

  private String obtenerToken(String correo) throws Exception {
    String respuesta =
        iniciarSesion(correo, CONTRASENA).andReturn().getResponse().getContentAsString();
    JsonNode cuerpo = objectMapper.readTree(respuesta);
    return cuerpo.get("token").asText();
  }
}
