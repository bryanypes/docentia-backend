package com.uniautonoma.docentia.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Lista de docentes con su promedio del periodo activo, según el alcance de quien consulta. */
class SeguimientoDocentesTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;

  private Facultad ingenieria;
  private Facultad derecho;
  private Usuario julian;
  private Usuario luisa;

  @BeforeEach
  void crearDocentes() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    derecho = escenario.crearFacultad("Derecho");
    julian =
        escenario.crearDocente("Julian Hoyos", escenario.crearPrograma("Software", ingenieria));
    luisa = escenario.crearDocente("Luisa Mosquera", escenario.crearPrograma("Leyes", derecho));
  }

  @Test
  void traeElPeriodoActivoYElPromedioSinNotasPorFuente() throws Exception {
    PeriodoAcademico periodo = escenario.crearPeriodoActivo("2026-2");
    for (FuenteEvaluacion fuente : FuenteEvaluacion.values()) {
      escenario.registrarCalificacion(julian, periodo, fuente, "4.0");
    }

    mockMvc
        .perform(get("/docentes/seguimiento").with(comoVicerrector()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodo.nombre").value("2026-2"))
        .andExpect(jsonPath("$.docentes[0].docente.nombreCompleto").value("Julian Hoyos"))
        .andExpect(jsonPath("$.docentes[0].promedio.estado").value("COMPLETO"))
        .andExpect(jsonPath("$.docentes[0].promedio.valor").value(4.0))
        .andExpect(jsonPath("$.docentes[0].notasPorFuente").doesNotExist())
        .andExpect(jsonPath("$.docentes[1].promedio.estado").value("INCOMPLETO"))
        .andExpect(jsonPath("$.docentes[1].promedio.valor").value(nullValue()))
        .andExpect(
            jsonPath(
                "$.docentes[1].promedio.fuentesPendientes",
                containsInAnyOrder("ESTUDIANTES", "COORDINACION", "VICERRECTORIA")));
  }

  @Test
  void elCoordinadorSoloVeSuFacultadAunquePidaOtra() throws Exception {
    escenario.crearPeriodoActivo("2026-2");

    mockMvc
        .perform(
            get("/docentes/seguimiento")
                .param("idFacultad", derecho.getIdFacultad().toString())
                .with(comoCoordinador(ingenieria.getIdFacultad())))
        .andExpect(jsonPath("$.docentes[*].docente.nombreCompleto", contains("Julian Hoyos")));
  }

  @Test
  void elVicerrectorPuedeFiltrarPorFacultad() throws Exception {
    escenario.crearPeriodoActivo("2026-2");

    mockMvc
        .perform(
            get("/docentes/seguimiento")
                .param("idFacultad", derecho.getIdFacultad().toString())
                .with(comoVicerrector()))
        .andExpect(jsonPath("$.docentes[*].docente.nombreCompleto", contains("Luisa Mosquera")));
  }

  @Test
  void sinPeriodoActivoElPeriodoYLosPromediosVanEnNull() throws Exception {
    mockMvc
        .perform(get("/docentes/seguimiento").with(comoVicerrector()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodo").value(nullValue()))
        .andExpect(jsonPath("$.docentes[0].promedio").value(nullValue()));
  }

  @Test
  void elDocenteNoTieneAcceso() throws Exception {
    mockMvc
        .perform(get("/docentes/seguimiento").with(como(julian)))
        .andExpect(status().isForbidden());
  }
}
