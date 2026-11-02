package com.uniautonoma.docentia.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.uniautonoma.docentia.PruebaIntegracion;
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

class SesionEstudianteTest extends PruebaIntegracion {

  private static final String CONTRASENA = "Clave-Segura-123";
  private static final String CORREO = "karen.mancilla@prueba.co";

  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private FacultadRepository facultadRepository;
  @Autowired private ProgramaRepository programaRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private Facultad ingenieria;
  private Programa software;

  @BeforeEach
  void crearEstudiante() {
    ingenieria = facultadRepository.save(new Facultad("Ingeniería"));
    software = programaRepository.save(new Programa("Ingeniería de Software", ingenieria));
    usuarioRepository.save(
        Usuario.crearEstudiante(
            "Karen Mancilla", CORREO, passwordEncoder.encode(CONTRASENA), software));
  }

  @Test
  void elLoginDelEstudianteIncluyeSuProgramaYLaFacultadDelPrograma() throws Exception {
    iniciarSesion(CORREO, CONTRASENA)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.usuario.rol").value("ESTUDIANTE"))
        .andExpect(jsonPath("$.usuario.idPrograma").value(software.getIdPrograma()))
        .andExpect(jsonPath("$.usuario.idFacultad").value(ingenieria.getIdFacultad()))
        .andExpect(jsonPath("$.usuario.contrasenaHash").doesNotExist());
  }

  @Test
  void conUnTokenValidoSeRecuperaLaSesionDelEstudiante() throws Exception {
    mockMvc
        .perform(get("/sesiones/actual").header(HttpHeaders.AUTHORIZATION, "Bearer " + token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rol").value("ESTUDIANTE"))
        .andExpect(jsonPath("$.correo").value(CORREO))
        .andExpect(jsonPath("$.idPrograma").value(software.getIdPrograma()))
        .andExpect(jsonPath("$.idFacultad").value(ingenieria.getIdFacultad()));
  }

  @Test
  void elEstudianteNoEntraALaGestionDeDocentes() throws Exception {
    mockMvc
        .perform(get("/docentes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.mensaje").value("No tienes permiso para realizar esta acción"));
  }

  @Test
  void elEstudianteNoAdministraUsuarios() throws Exception {
    mockMvc
        .perform(get("/usuarios").header(HttpHeaders.AUTHORIZATION, "Bearer " + token()))
        .andExpect(status().isForbidden());
  }

  @Test
  void unaContrasenaIncorrectaRespondeNoAutenticado() throws Exception {
    iniciarSesion(CORREO, "otra-clave")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
  }

  @Test
  void sinTokenLaSesionActualRespondeNoAutenticado() throws Exception {
    mockMvc.perform(get("/sesiones/actual")).andExpect(status().isUnauthorized());
  }

  private ResultActions iniciarSesion(String correo, String contrasena) throws Exception {
    return mockMvc.perform(
        post("/sesiones")
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new InicioSesionRequest(correo, contrasena))));
  }

  private String token() throws Exception {
    String respuesta =
        iniciarSesion(CORREO, CONTRASENA).andReturn().getResponse().getContentAsString();
    JsonNode cuerpo = objectMapper.readTree(respuesta);
    return cuerpo.get("token").asText();
  }
}
