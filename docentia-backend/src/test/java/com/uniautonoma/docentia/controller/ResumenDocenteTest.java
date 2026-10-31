package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.NotasAspectos;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CompromisoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ResumenDocenteTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CompromisoRepository compromisoRepository;

  private Programa software;
  private Usuario docente;
  private PeriodoAcademico periodo;

  @BeforeEach
  void crearDocenteYPeriodo() {
    software =
        escenario.crearPrograma("Ingeniería de Software", escenario.crearFacultad("Ingeniería"));
    docente = escenario.crearDocente("Julian Hoyos", software);
    periodo = escenario.crearPeriodoActivo("2026-2");
  }

  @Test
  void sinNotaDeEstudiantesTodasLasDimensionesQuedanIncompletas() throws Exception {
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.COORDINACION, "4.7");
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.VICERRECTORIA, "2.3");

    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dimensiones", hasSize(5)))
        .andExpect(jsonPath("$.dimensiones[*].estado", everyItem(is("INCOMPLETO"))))
        .andExpect(jsonPath("$.dimensiones[0].nombre").value("Dominio disciplinar"))
        .andExpect(jsonPath("$.dimensiones[0].resultado").doesNotExist());
  }

  @Test
  void conLasTresFuentesVeElResultadoPonderadoDeCadaDimensionPeroNoSusPartes() throws Exception {
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.ESTUDIANTES, "4.0");
    escenario.registrarCalificacion(
        docente,
        periodo,
        FuenteEvaluacion.COORDINACION,
        new NotasAspectos(
            new BigDecimal("4.7"),
            new BigDecimal("3.0"),
            new BigDecimal("4.7"),
            new BigDecimal("4.7"),
            new BigDecimal("4.7")));
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.VICERRECTORIA, "2.3");

    // Planeación académica: 4.0 * 0.5 + 3.0 * 0.3 + 2.3 * 0.2 = 3.36
    String respuesta =
        mockMvc
            .perform(get("/expedientes/propio").with(como(docente)))
            .andExpect(jsonPath("$.dimensiones[1].dimension").value("PLANEACION_ACADEMICA"))
            .andExpect(jsonPath("$.dimensiones[1].estado").value("COMPLETO"))
            .andExpect(jsonPath("$.dimensiones[1].resultado").value(3.36))
            .andExpect(jsonPath("$.promedio.estado").value("COMPLETO"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(respuesta)
        .doesNotContain("4.7", "2.3")
        .doesNotContain(
            "notaCoordinacion", "notaVicerrectoria", "notaEstudiantes", "notasPorFuente");
  }

  @Test
  void veSusCompromisosVigentesYNoLosDeOtroDocente() throws Exception {
    Usuario otroDocente = escenario.crearDocente("Diana Astaiza", software);
    compromisoRepository.save(
        new Compromiso(docente, periodo, "Entregar notas a tiempo", LocalDate.of(2026, 10, 15)));
    compromisoRepository.saveAndFlush(
        new Compromiso(otroDocente, periodo, "Compromiso ajeno", LocalDate.of(2026, 10, 1)));

    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(
            jsonPath("$.compromisosVigentes[*].descripcion", contains("Entregar notas a tiempo")));
  }
}
