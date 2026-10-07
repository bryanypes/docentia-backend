package com.uniautonoma.docentia.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.PonderacionRequest;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.NotasAspectos;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class PromedioDocenteTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;

  private PeriodoAcademico periodo;
  private Usuario docente;

  @BeforeEach
  void crearDocenteYPeriodo() {
    docente =
        escenario.crearDocente(
            "Julian Hoyos",
            escenario.crearPrograma(
                "Ingeniería de Software", escenario.crearFacultad("Ingeniería")));
    periodo = escenario.crearPeriodoActivo("2026-2");
  }

  @Test
  void conCoordinacionYVicerrectoriaPeroSinEstudiantesElPromedioEsIncompleto() throws Exception {
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.COORDINACION, "3.0");
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.VICERRECTORIA, "5.0");

    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.promedio.estado").value("INCOMPLETO"))
        .andExpect(jsonPath("$.promedio.valor").doesNotExist())
        .andExpect(jsonPath("$.promedio.fuentesPendientes", contains("ESTUDIANTES")));
  }

  @Test
  void conLasTresFuentesSeCalculaConLosPesosPredeterminados() throws Exception {
    registrarLasTresFuentes();

    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.promedio.estado").value("COMPLETO"))
        .andExpect(jsonPath("$.promedio.valor").value(3.9))
        .andExpect(jsonPath("$.promedio.fuentesPendientes", empty()));
  }

  @Test
  void elPromedioCambiaCuandoElVicerrectorConfiguraOtrosPesos() throws Exception {
    registrarLasTresFuentes();
    PonderacionRequest pesos =
        new PonderacionRequest(new BigDecimal("40"), new BigDecimal("40"), new BigDecimal("20"));
    mockMvc
        .perform(
            put("/periodos-academicos/activo/ponderaciones")
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(pesos)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(jsonPath("$.promedio.valor").value(3.8));
  }

  @Test
  void laNotaDeCadaFuenteEsElPromedioDeSusCincoAspectos() throws Exception {
    // (5.0 + 4.0 + 3.0 + 4.0 + 4.5) / 5 = 4.1 en cada fuente, así que el ponderado también es 4.1
    NotasAspectos notas =
        new NotasAspectos(
            new BigDecimal("5.0"),
            new BigDecimal("4.0"),
            new BigDecimal("3.0"),
            new BigDecimal("4.0"),
            new BigDecimal("4.5"));
    for (FuenteEvaluacion fuente : FuenteEvaluacion.values()) {
      escenario.registrarCalificacion(docente, periodo, fuente, notas);
    }

    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(jsonPath("$.promedio.valor").value(4.1));
  }

  @Test
  void lasCalificacionesDeOtroPeriodoNoCuentan() throws Exception {
    PeriodoAcademico anterior = escenario.crearPeriodoCerrado("2026-1", LocalDate.of(2026, 2, 2));
    for (FuenteEvaluacion fuente : FuenteEvaluacion.values()) {
      escenario.registrarCalificacion(docente, anterior, fuente, "4.0");
    }

    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(jsonPath("$.promedio.estado").value("INCOMPLETO"));
  }

  private void registrarLasTresFuentes() {
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.ESTUDIANTES, "4.0");
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.COORDINACION, "3.0");
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.VICERRECTORIA, "5.0");
  }
}
