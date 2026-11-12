package com.uniautonoma.docentia.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** Con SMTP configurado la recuperación ofrece el enlace por correo. */
@TestPropertySource(properties = "docentia.notificaciones.correo.host=smtp.prueba.invalid")
class RecuperacionConSmtpTest extends PruebaIntegracion {

  @Test
  void conSmtpLaPantallaOfreceElEnlacePorCorreo() throws Exception {
    mockMvc
        .perform(get("/sesiones/recuperacion/disponibilidad"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.porCorreo").value(true));
  }
}
