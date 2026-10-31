package com.uniautonoma.docentia.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.InicioSesionGoogleRequest;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.FacultadRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.VerificadorTokenGoogle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
    properties = {"docentia.google.client-id=cliente-de-prueba.apps.googleusercontent.com"})
class SesionGoogleControllerTest extends PruebaIntegracion {

  private static final String CORREO_COORDINADOR = "marta.ruiz@uniautonoma.edu.co";
  private static final String TOKEN_GOOGLE = "token-simulado";

  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private FacultadRepository facultadRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @MockBean private VerificadorTokenGoogle verificadorTokenGoogle;

  @BeforeEach
  void crearUsuario() {
    Facultad ingenieria = facultadRepository.save(new Facultad("Ingeniería"));
    usuarioRepository.save(
        Usuario.crearCoordinador(
            "Marta Ruiz",
            CORREO_COORDINADOR,
            passwordEncoder.encode("Clave-Segura-123"),
            ingenieria));
  }

  @Test
  void elLoginConGoogleDevuelveLaMismaSesionQueElCorreoRegistrado() throws Exception {
    when(verificadorTokenGoogle.validarYExtraerCorreo(TOKEN_GOOGLE)).thenReturn(CORREO_COORDINADOR);

    mockMvc
        .perform(
            post("/sesiones/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new InicioSesionGoogleRequest(TOKEN_GOOGLE))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.usuario.correo").value(CORREO_COORDINADOR))
        .andExpect(jsonPath("$.usuario.rol").value("COORDINADOR"));
  }

  @Test
  void unCorreoFueraDelDominioInstitucionalRespondeComoCredencialesInvalidas() throws Exception {
    when(verificadorTokenGoogle.validarYExtraerCorreo(TOKEN_GOOGLE))
        .thenReturn("persona@gmail.com");

    mockMvc
        .perform(
            post("/sesiones/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new InicioSesionGoogleRequest(TOKEN_GOOGLE))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
  }

  @Test
  void unCorreoInstitucionalNoRegistradoRespondeComoCredencialesInvalidas() throws Exception {
    when(verificadorTokenGoogle.validarYExtraerCorreo(TOKEN_GOOGLE))
        .thenReturn("nadie@uniautonoma.edu.co");

    mockMvc
        .perform(
            post("/sesiones/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new InicioSesionGoogleRequest(TOKEN_GOOGLE))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unTokenGoogleInvalidoRespondeNoAutorizado() throws Exception {
    when(verificadorTokenGoogle.validarYExtraerCorreo(anyString()))
        .thenThrow(new com.uniautonoma.docentia.exception.CredencialesInvalidasException());

    mockMvc
        .perform(
            post("/sesiones/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new InicioSesionGoogleRequest("token-mal"))))
        .andExpect(status().isUnauthorized());
  }
}
