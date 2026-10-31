package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

class DocenteControllerTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;

  private Facultad ingenieria;
  private Facultad derecho;
  private Programa programaDerecho;
  private Usuario docenteIngenieria;
  private Usuario docenteDerecho;

  @BeforeEach
  void crearDocentesEnDosFacultades() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    derecho = escenario.crearFacultad("Derecho");
    Programa software = escenario.crearPrograma("Ingeniería de Software", ingenieria);
    programaDerecho = escenario.crearPrograma("Derecho", derecho);
    docenteIngenieria = escenario.crearDocente("Julian Hoyos", software);
    docenteDerecho = escenario.crearDocente("Luisa Mosquera", programaDerecho);
  }

  @Test
  void elCoordinadorSoloListaLosDocentesDeSuFacultad() throws Exception {
    mockMvc
        .perform(get("/docentes").with(comoCoordinador(ingenieria.getIdFacultad())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].nombreCompleto", contains("Julian Hoyos")));
  }

  @Test
  void elCoordinadorNoAmpliaSuAlcancePidiendoOtraFacultad() throws Exception {
    mockMvc
        .perform(
            get("/docentes")
                .param("idFacultad", derecho.getIdFacultad().toString())
                .with(comoCoordinador(ingenieria.getIdFacultad())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].nombreCompleto", contains("Julian Hoyos")));
  }

  @Test
  void elCoordinadorConsultaUnDocenteDeSuFacultad() throws Exception {
    mockMvc
        .perform(
            get("/docentes/{id}", docenteIngenieria.getIdUsuario())
                .with(comoCoordinador(ingenieria.getIdFacultad())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombreFacultad").value("Ingeniería"))
        .andExpect(jsonPath("$.contrasenaHash").doesNotExist());
  }

  @Test
  void unDocenteDeOtraFacultadRespondeIgualQueUnoQueNoExiste() throws Exception {
    MvcResult deOtraFacultad =
        mockMvc
            .perform(
                get("/docentes/{id}", docenteDerecho.getIdUsuario())
                    .with(comoCoordinador(ingenieria.getIdFacultad())))
            .andExpect(status().isNotFound())
            .andReturn();
    MvcResult inexistente =
        mockMvc
            .perform(
                get("/docentes/{id}", 999_999).with(comoCoordinador(ingenieria.getIdFacultad())))
            .andExpect(status().isNotFound())
            .andReturn();

    assertThat(deOtraFacultad.getResponse().getContentAsString())
        .isEqualTo(inexistente.getResponse().getContentAsString());
  }

  @Test
  void elVicerrectorVeLosDocentesDeTodasLasFacultades() throws Exception {
    mockMvc
        .perform(get("/docentes").with(comoVicerrector()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].nombreCompleto", contains("Julian Hoyos", "Luisa Mosquera")));

    mockMvc
        .perform(get("/docentes/{id}", docenteDerecho.getIdUsuario()).with(comoVicerrector()))
        .andExpect(status().isOk());
  }

  @Test
  void elDocenteNoPuedeConsultarElListadoDeDocentes() throws Exception {
    mockMvc
        .perform(get("/docentes").with(como(docenteIngenieria)))
        .andExpect(status().isForbidden());
  }

  @Test
  void unCoordinadorNoCuentaComoDocente() throws Exception {
    Usuario coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);

    mockMvc
        .perform(get("/docentes/{id}", coordinador.getIdUsuario()).with(comoVicerrector()))
        .andExpect(status().isNotFound());
  }

  @Test
  void elCoordinadorSoloVeSuFacultadYSusProgramas() throws Exception {
    mockMvc
        .perform(get("/facultades").with(comoCoordinador(ingenieria.getIdFacultad())))
        .andExpect(jsonPath("$[*].nombre", contains("Ingeniería")));
    mockMvc
        .perform(
            get("/facultades/{id}", derecho.getIdFacultad())
                .with(comoCoordinador(ingenieria.getIdFacultad())))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get("/programas")
                .param("idFacultad", derecho.getIdFacultad().toString())
                .with(comoCoordinador(ingenieria.getIdFacultad())))
        .andExpect(jsonPath("$[*].nombre", contains("Ingeniería de Software")));
    mockMvc
        .perform(
            get("/programas/{id}", programaDerecho.getIdPrograma())
                .with(comoCoordinador(ingenieria.getIdFacultad())))
        .andExpect(status().isNotFound());
  }
}
