package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.PonderacionRequest;
import com.uniautonoma.docentia.repository.PonderacionRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class PonderacionControllerTest extends PruebaIntegracion {

  private static final String RUTA = "/periodos-academicos/activo/ponderaciones";

  @Autowired private EscenarioPrueba escenario;
  @Autowired private PonderacionRepository ponderacionRepository;

  @Test
  void sinConfigurarSeUsanLosPesosPredeterminados() throws Exception {
    escenario.crearPeriodoActivo("2026-2");

    mockMvc
        .perform(get(RUTA).with(comoVicerrector()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodo.nombre").value("2026-2"))
        .andExpect(jsonPath("$.pesoEstudiantes").value(50))
        .andExpect(jsonPath("$.pesoCoordinacion").value(30))
        .andExpect(jsonPath("$.pesoVicerrectoria").value(20))
        .andExpect(jsonPath("$.esPredeterminada").value(true));
  }

  @Test
  void elVicerrectorConfiguraPesosQueSumanCien() throws Exception {
    escenario.crearPeriodoActivo("2026-2");

    configurar(pesos("40", "35.5", "24.5"), comoVicerrector())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pesoCoordinacion").value(35.5))
        .andExpect(jsonPath("$.esPredeterminada").value(false));

    mockMvc
        .perform(get(RUTA).with(comoVicerrector()))
        .andExpect(jsonPath("$.pesoEstudiantes").value(40.0))
        .andExpect(jsonPath("$.esPredeterminada").value(false));
  }

  @Test
  void configurarDeNuevoReemplazaLaPonderacionDelPeriodo() throws Exception {
    escenario.crearPeriodoActivo("2026-2");
    configurar(pesos("40", "40", "20"), comoVicerrector());

    configurar(pesos("60", "20", "20"), comoVicerrector()).andExpect(status().isOk());

    assertThat(ponderacionRepository.count()).isEqualTo(1);
  }

  @Test
  void rechazaPesosQueNoSumanCien() throws Exception {
    escenario.crearPeriodoActivo("2026-2");

    configurar(pesos("50", "30", "10"), comoVicerrector())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Las ponderaciones deben sumar 100%"));
    configurar(pesos("50", "30", "20.01"), comoVicerrector()).andExpect(status().isBadRequest());

    assertThat(ponderacionRepository.count()).isZero();
  }

  @Test
  void rechazaPesosNegativosAunqueLaSumaDeCien() throws Exception {
    escenario.crearPeriodoActivo("2026-2");

    configurar(pesos("120", "-20", "0"), comoVicerrector())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.pesoEstudiantes").exists())
        .andExpect(jsonPath("$.errores.pesoCoordinacion").exists());
  }

  @Test
  void sinPeriodoActivoNoHayPonderacionQueConfigurar() throws Exception {
    configurar(pesos("50", "30", "20"), comoVicerrector()).andExpect(status().isConflict());
    mockMvc.perform(get(RUTA).with(comoVicerrector())).andExpect(status().isConflict());
  }

  @Test
  void elCoordinadorConsultaPeroNoConfigura() throws Exception {
    escenario.crearPeriodoActivo("2026-2");
    Long idFacultad = escenario.crearFacultad("Ingeniería").getIdFacultad();

    mockMvc.perform(get(RUTA).with(comoCoordinador(idFacultad))).andExpect(status().isOk());
    configurar(pesos("50", "30", "20"), comoCoordinador(idFacultad))
        .andExpect(status().isForbidden());
  }

  private ResultActions configurar(PonderacionRequest solicitud, RequestPostProcessor sesion)
      throws Exception {
    return mockMvc.perform(
        put(RUTA).with(sesion).contentType(MediaType.APPLICATION_JSON).content(aJson(solicitud)));
  }

  private PonderacionRequest pesos(String estudiantes, String coordinacion, String vicerrectoria) {
    return new PonderacionRequest(
        new BigDecimal(estudiantes), new BigDecimal(coordinacion), new BigDecimal(vicerrectoria));
  }
}
