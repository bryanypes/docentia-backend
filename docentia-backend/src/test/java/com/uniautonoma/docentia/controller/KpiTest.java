package com.uniautonoma.docentia.controller;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * I5. Escenario: en 2026-2 Julián (Ingeniería) promedia 4.60, Diana (Ingeniería) 4.08 y Luisa
 * (Derecho) 3.00; en 2026-1 solo Julián tiene promedio completo (4.00) y Luisa una nota suelta.
 */
class KpiTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;

  private Facultad ingenieria;
  private Facultad derecho;
  private PeriodoAcademico anterior;
  private PeriodoAcademico activo;
  private Usuario julian;

  @BeforeEach
  void crearDosPeriodosEvaluados() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    derecho = escenario.crearFacultad("Derecho");
    julian =
        escenario.crearDocente("Julian Hoyos", escenario.crearPrograma("Software", ingenieria));
    Usuario diana =
        escenario.crearDocente("Diana Astaiza", escenario.crearPrograma("Electrónica", ingenieria));
    Usuario luisa =
        escenario.crearDocente("Luisa Mosquera", escenario.crearPrograma("Derecho", derecho));
    anterior = escenario.crearPeriodoCerrado("2026-1", LocalDate.of(2026, 2, 2));
    activo = escenario.crearPeriodoActivo("2026-2");

    calificarIgual(julian, anterior, "4.0");
    escenario.registrarCalificacion(luisa, anterior, FuenteEvaluacion.COORDINACION, "3.5");

    calificarIgual(julian, activo, "4.6");
    // 4.0*50% + 4.0*30% + 4.4*20% = 4.08
    escenario.registrarCalificacion(diana, activo, FuenteEvaluacion.ESTUDIANTES, "4.0");
    escenario.registrarCalificacion(diana, activo, FuenteEvaluacion.COORDINACION, "4.0");
    escenario.registrarCalificacion(diana, activo, FuenteEvaluacion.VICERRECTORIA, "4.4");
    calificarIgual(luisa, activo, "3.0");
  }

  @Test
  void elVicerrectorVeLosIndicadoresInstitucionalesComparadosConElPeriodoAnterior()
      throws Exception {
    mockMvc
        .perform(get("/kpis").with(comoVicerrector()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodo.nombre").value("2026-2"))
        .andExpect(jsonPath("$.periodoAnterior.nombre").value("2026-1"))
        .andExpect(jsonPath("$.idFacultad").doesNotExist())
        .andExpect(jsonPath("$.umbralDestacado").value(4.5))
        .andExpect(jsonPath("$.docentesEvaluados.valor").value(3))
        .andExpect(jsonPath("$.docentesEvaluados.valorAnterior").value(1))
        .andExpect(jsonPath("$.docentesEvaluados.variacion").value(2))
        // (4.60 + 4.08 + 3.00) / 3 = 3.893 -> 3.89; antes 4.00
        .andExpect(jsonPath("$.promedioGeneral.valor").value(3.89))
        .andExpect(jsonPath("$.promedioGeneral.valorAnterior").value(4.0))
        .andExpect(jsonPath("$.promedioGeneral.variacion").value(-0.11))
        .andExpect(jsonPath("$.evaluacionesRealizadas.valor").value(9))
        .andExpect(jsonPath("$.evaluacionesRealizadas.valorAnterior").value(4))
        .andExpect(jsonPath("$.docentesDestacados.valor").value(1))
        .andExpect(jsonPath("$.docentesDestacados.valorAnterior").value(0))
        .andExpect(jsonPath("$.docentesDestacados.variacion").value(1));
  }

  @Test
  void elCoordinadorSoloVeSuFacultadAunquePidaOtra() throws Exception {
    mockMvc
        .perform(
            get("/kpis")
                .param("facultadId", derecho.getIdFacultad().toString())
                .with(comoCoordinador(ingenieria.getIdFacultad())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idFacultad").value(ingenieria.getIdFacultad()))
        .andExpect(jsonPath("$.docentesEvaluados.valor").value(2))
        .andExpect(jsonPath("$.promedioGeneral.valor").value(4.34))
        .andExpect(jsonPath("$.evaluacionesRealizadas.valor").value(6))
        .andExpect(jsonPath("$.evaluacionesRealizadas.valorAnterior").value(3))
        .andExpect(jsonPath("$.docentesDestacados.valor").value(1));
  }

  @Test
  void sinPromediosCompletosElPromedioAnteriorEsNuloAunqueHayaEvaluaciones() throws Exception {
    mockMvc
        .perform(
            get("/kpis")
                .param("facultadId", derecho.getIdFacultad().toString())
                .with(comoVicerrector()))
        .andExpect(jsonPath("$.docentesEvaluados.valor").value(1))
        .andExpect(jsonPath("$.docentesEvaluados.valorAnterior").value(0))
        .andExpect(jsonPath("$.promedioGeneral.valor").value(3.0))
        .andExpect(jsonPath("$.promedioGeneral.valorAnterior").doesNotExist())
        .andExpect(jsonPath("$.promedioGeneral.variacion").doesNotExist())
        .andExpect(jsonPath("$.evaluacionesRealizadas.valorAnterior").value(1));
  }

  @Test
  void sinPeriodoAnteriorLosValoresAnterioresSonNulosNoCero() throws Exception {
    mockMvc
        .perform(
            get("/kpis")
                .param("periodoId", anterior.getIdPeriodo().toString())
                .with(comoVicerrector()))
        .andExpect(jsonPath("$.periodo.nombre").value("2026-1"))
        .andExpect(jsonPath("$.periodoAnterior").doesNotExist())
        .andExpect(jsonPath("$.docentesEvaluados.valor").value(1))
        .andExpect(jsonPath("$.docentesEvaluados.valorAnterior").doesNotExist())
        .andExpect(jsonPath("$.docentesEvaluados.variacion").doesNotExist())
        .andExpect(jsonPath("$.docentesDestacados.valor").value(0));
  }

  @Test
  void unPeriodoAnteriorSinDatosEnElAlcanceCuentaComoSinDato() throws Exception {
    Facultad ciencias = escenario.crearFacultad("Ciencias");
    Usuario nuevo =
        escenario.crearDocente("Ana Ruiz", escenario.crearPrograma("Biología", ciencias));
    calificarIgual(nuevo, activo, "4.5");

    mockMvc
        .perform(
            get("/kpis")
                .param("facultadId", ciencias.getIdFacultad().toString())
                .with(comoVicerrector()))
        .andExpect(jsonPath("$.periodoAnterior.nombre").value("2026-1"))
        .andExpect(jsonPath("$.evaluacionesRealizadas.valor").value(3))
        .andExpect(jsonPath("$.evaluacionesRealizadas.valorAnterior").doesNotExist())
        // 4.50 está justo en el umbral: cuenta como destacado.
        .andExpect(jsonPath("$.docentesDestacados.valor").value(1))
        .andExpect(jsonPath("$.docentesDestacados.valorAnterior").doesNotExist());
  }

  @Test
  void elDocenteNoAccedeYLosFiltrosInexistentesRespondenNoEncontrado() throws Exception {
    mockMvc.perform(get("/kpis").with(como(julian))).andExpect(status().isForbidden());
    mockMvc
        .perform(get("/kpis").param("periodoId", "999999").with(comoVicerrector()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/kpis").param("facultadId", "999999").with(comoVicerrector()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/kpis").param("periodoId", "abc").with(comoVicerrector()))
        .andExpect(status().isBadRequest());
  }

  private void calificarIgual(Usuario docente, PeriodoAcademico periodo, String nota) {
    for (FuenteEvaluacion fuente : FuenteEvaluacion.values()) {
      escenario.registrarCalificacion(docente, periodo, fuente, nota);
    }
  }
}
