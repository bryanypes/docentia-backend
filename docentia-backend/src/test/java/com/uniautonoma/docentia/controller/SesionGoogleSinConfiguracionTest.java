package com.uniautonoma.docentia.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.InicioSesionGoogleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"docentia.google.client-id="})
class SesionGoogleSinConfiguracionTest extends PruebaIntegracion {

  @Test
  void sinClientIdConfiguradoRespondeServicioNoDisponible() throws Exception {
    mockMvc
        .perform(
            post("/sesiones/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new InicioSesionGoogleRequest("cualquier-token"))))
        .andExpect(status().isServiceUnavailable())
        .andExpect(
            jsonPath("$.mensaje")
                .value("El inicio de sesión con Google no está configurado en este ambiente"));
  }
}
