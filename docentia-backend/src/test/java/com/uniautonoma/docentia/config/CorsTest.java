package com.uniautonoma.docentia.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.PruebaIntegracion;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;

/** CORS por propiedad: solo los orígenes listados, sin comodines y sin credenciales. */
@TestPropertySource(properties = "docentia.cors.origenes-permitidos=http://docentia.prueba")
class CorsTest extends PruebaIntegracion {

  private static final String PERMITIDO = "http://docentia.prueba";

  @Test
  void laConsultaPreviaDeUnOrigenListadoSeAceptaConAuthorization() throws Exception {
    mockMvc
        .perform(
            options("/docentes")
                .header(HttpHeaders.ORIGIN, PERMITIDO)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PERMITIDO))
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
  }

  @Test
  void unOrigenNoListadoSeRechaza() throws Exception {
    mockMvc
        .perform(
            options("/docentes")
                .header(HttpHeaders.ORIGIN, "http://otro.prueba")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void laPeticionRealDeUnOrigenListadoSigueExigiendoToken() throws Exception {
    mockMvc
        .perform(get("/docentes").header(HttpHeaders.ORIGIN, PERMITIDO))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PERMITIDO));
  }

  @Test
  void sinOrigenesConfiguradosNoHayCors() {
    MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/docentes");
    peticion.addHeader(HttpHeaders.ORIGIN, PERMITIDO);

    assertThat(
            SecurityConfig.fuenteCors(new CorsProperties(List.of(" ")))
                .getCorsConfiguration(peticion))
        .isNull();
  }

  @Test
  void rechazaComodines() {
    assertThatThrownBy(() -> new CorsProperties(List.of("*")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new CorsProperties(List.of("https://*.uniautonoma.edu.co")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
