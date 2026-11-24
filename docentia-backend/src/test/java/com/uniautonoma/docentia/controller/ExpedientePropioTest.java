package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Usuario;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ExpedientePropioTest extends PruebaIntegracion {

  // Valores poco comunes a propósito: si alguno aparece en la respuesta es porque se filtró.
  private static final String NOTA_COORDINACION = "4.7";
  private static final String NOTA_AUTOEVALUACION = "2.3";

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
        docente, periodo, FuenteEvaluacion.AUTOEVALUACION, NOTA_AUTOEVALUACION);

    String respuesta =
        mockMvc
            .perform(get("/expedientes/propio").with(como(docente)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.docente.nombreCompleto").value("Julian Hoyos"))
            .andExpect(jsonPath("$.periodo.nombre").value("2026-2"))
            .andExpect(jsonPath("$.promedio.estado").value("PARCIAL"))
            .andExpect(jsonPath("$.promedio.valor").doesNotExist())
            .andExpect(jsonPath("$.promedio.fuentesPendientes", contains("ESTUDIANTES")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(respuesta)
        .doesNotContain(NOTA_COORDINACION, NOTA_AUTOEVALUACION)
        .doesNotContain("Observación reservada", "observacion")
        .doesNotContain("dominioDisciplinar", "notasPorFuente", "notaCoordinacion", "evaluador");
  }

  @Test
  void elDocenteNoVeUnPromedioParcialPorqueRevelariaLaNotaDeUnaFuente() throws Exception {
    PeriodoAcademico periodo = escenario.crearPeriodoActivo("2026-2");
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.COORDINACION, "3.2");

    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(jsonPath("$.promedio.estado").value("PARCIAL"))
        .andExpect(jsonPath("$.promedio.valor").doesNotExist())
        .andExpect(jsonPath("$.promedio.categoria").doesNotExist())
        .andExpect(jsonPath("$.dimensiones[0].resultado").doesNotExist());
  }

  @Test
  void elDocenteNoPuedeLeerLasCalificacionesPorLosEndpointsDeCadaFuente() throws Exception {
    escenario.crearPeriodoActivo("2026-2");
    List<String> rutasReservadas =
        List.of("/docentes/{id}/calificaciones/coordinacion", "/docentes/{id}");

    for (String ruta : rutasReservadas) {
      mockMvc
          .perform(get(ruta, docente.getIdUsuario()).with(como(docente)))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void sinPeriodoActivoElExpedienteLlegaSinPeriodoNiPromedio() throws Exception {
    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodo").doesNotExist())
        .andExpect(jsonPath("$.promedio").doesNotExist());
  }

  @Test
  void elExpedientePropioEsSoloParaLosEvaluados() throws Exception {
    mockMvc
        .perform(get("/expedientes/propio").with(comoVicerrector()))
        .andExpect(status().isForbidden());
    // El coordinador también es docente evaluado y tiene su expediente propio.
    mockMvc
        .perform(
            get("/expedientes/propio")
                .with(como(escenario.crearCoordinador("Marta Ruiz", ingenieria))))
        .andExpect(status().isOk());
  }

  @Test
  void elDocenteRecibeSoloSuPromedioFinalEnElHistorico() throws Exception {
    PeriodoAcademico anterior = escenario.crearPeriodoCerrado("2025-2", LocalDate.of(2025, 8, 4));
    escenario.registrarCalificacion(docente, anterior, FuenteEvaluacion.ESTUDIANTES, "4.0");
    escenario.registrarCalificacion(docente, anterior, FuenteEvaluacion.COORDINACION, "3.5");
    escenario.registrarCalificacion(docente, anterior, FuenteEvaluacion.AUTOEVALUACION, "4.5");

    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.historico", hasSize(1)))
        .andExpect(jsonPath("$.historico[0].periodo.nombre").value("2025-2"))
        .andExpect(jsonPath("$.historico[0].promedio.estado").value("COMPLETO"))
        .andExpect(jsonPath("$.historico[0].promedio.valor").value(3.95))
        .andExpect(jsonPath("$.historico[0].notasPorFuente").doesNotExist());
  }
}
