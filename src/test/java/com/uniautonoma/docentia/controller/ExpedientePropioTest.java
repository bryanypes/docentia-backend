package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Usuario;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ExpedientePropioTest extends PruebaIntegracion {

  // Valores poco comunes a propósito: si alguno aparece en la respuesta es porque se filtró.
  private static final String NOTA_COORDINACION = "4.7";
  private static final String NOTA_VICERRECTORIA = "2.3";

  @Autowired private EscenarioPrueba escenario;

  private Facultad ingenieria;
  private Usuario docente;

  @BeforeEach
  void crearDocente() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    docente =
        escenario.crearDocente(
            "Julian Hoyos", escenario.crearPrograma("Ingeniería de Software", ingenieria));
  }

  @Test
  void elExpedienteDelDocenteNoTraeLasNotasDeCoordinacionNiDeVicerrectoria() throws Exception {
    PeriodoAcademico periodo = escenario.crearPeriodoActivo("2026-2");
    escenario.registrarCalificacion(
        docente, periodo, FuenteEvaluacion.COORDINACION, NOTA_COORDINACION);
    escenario.registrarCalificacion(
        docente, periodo, FuenteEvaluacion.VICERRECTORIA, NOTA_VICERRECTORIA);

    String respuesta =
        mockMvc
            .perform(get("/expedientes/propio").with(como(docente)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.docente.nombreCompleto").value("Julian Hoyos"))
            .andExpect(jsonPath("$.periodo.nombre").value("2026-2"))
            .andExpect(jsonPath("$.fuentesPendientes", contains("ESTUDIANTES")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(respuesta)
        .doesNotContain(NOTA_COORDINACION, NOTA_VICERRECTORIA)
        .doesNotContain("Observación reservada", "observacion")
        .doesNotContain("dominioDisciplinar", "notasPorFuente", "evaluador");
  }

  @Test
  void elDocenteNoPuedeLeerLasCalificacionesPorLosEndpointsDeCadaFuente() throws Exception {
    escenario.crearPeriodoActivo("2026-2");
    List<String> rutasReservadas =
        List.of(
            "/docentes/{id}/calificaciones/coordinacion",
            "/docentes/{id}/calificaciones/vicerrectoria",
            "/docentes/{id}");

    for (String ruta : rutasReservadas) {
      mockMvc
          .perform(get(ruta, docente.getIdUsuario()).with(como(docente)))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void sinPeriodoActivoElExpedienteLlegaSinPeriodoNiPendientes() throws Exception {
    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodo").doesNotExist())
        .andExpect(jsonPath("$.fuentesPendientes", empty()));
  }

  @Test
  void elExpedientePropioEsSoloParaDocentes() throws Exception {
    mockMvc
        .perform(get("/expedientes/propio").with(comoVicerrector()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/expedientes/propio").with(comoCoordinador(ingenieria.getIdFacultad())))
        .andExpect(status().isForbidden());
  }
}
