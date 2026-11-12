package com.uniautonoma.docentia.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.security.UsuarioSesion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "docentia.proceso.exigir-asistencia=true")
class ComparativoProgramaTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;

  private Facultad facultad;
  private Programa programa;
  private Usuario docente;
  private Usuario par;
  private PeriodoAcademico periodo;

  @BeforeEach
  void crearEscenario() {
    facultad = escenario.crearFacultad("Facultad comparativo");
    programa = escenario.crearPrograma("Programa comparativo", facultad);
    docente = escenario.crearDocente("Docente comparativo", programa);
    par = escenario.crearDocente("Par comparativo", programa);
    periodo = escenario.crearPeriodoActivo("Comparativo 2026");
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.ESTUDIANTES, "4.0");
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.COORDINACION, "3.0");
    escenario.registrarCalificacion(par, periodo, FuenteEvaluacion.COORDINACION, "5.0");
    escenario.registrarCalificacion(par, periodo, FuenteEvaluacion.AUTOEVALUACION, "3.0");
  }

  @Test
  void usaSoloNotasRealesYRespetaElAlcanceDeCoordinacion() throws Exception {
    mockMvc
        .perform(
            get("/expedientes/{id}/comparativo", docente.getIdUsuario())
                .with(comoCoordinador(facultad.getIdFacultad())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombrePrograma").value("Programa comparativo"))
        .andExpect(
            jsonPath(
                "$.fuentes[*].fuente", contains("ESTUDIANTES", "COORDINACION", "AUTOEVALUACION")))
        .andExpect(jsonPath("$.fuentes[0].notaDocente").value(4.0))
        .andExpect(jsonPath("$.fuentes[0].promedioPrograma").value(4.0))
        .andExpect(jsonPath("$.fuentes[1].notaDocente").value(3.0))
        .andExpect(jsonPath("$.fuentes[1].promedioPrograma").value(4.0))
        .andExpect(jsonPath("$.fuentes[2].notaDocente").doesNotExist())
        .andExpect(jsonPath("$.fuentes[2].promedioPrograma").value(3.0));

    mockMvc
        .perform(
            get("/expedientes/{id}/comparativo", docente.getIdUsuario()).with(comoVicerrector()))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/expedientes/{id}/comparativo", docente.getIdUsuario())
                .with(comoCoordinador(escenario.crearFacultad("Otra facultad").getIdFacultad())))
        .andExpect(status().isNotFound());
  }

  @Test
  void elDocenteEsperaLaReunionYElVisualizadorNoObtieneNotasIndividuales() throws Exception {
    mockMvc
        .perform(get("/expedientes/propio/comparativo").with(como(docente)))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.mensaje")
                .value(
                    "El comparativo estará disponible cuando la coordinación habilite tu resultado"));

    mockMvc
        .perform(
            get("/expedientes/{id}/comparativo", docente.getIdUsuario())
                .with(
                    authentication(
                        new UsuarioSesion(
                                9003L,
                                "Visualizador de prueba",
                                "visualizador@prueba.co",
                                Rol.VISUALIZADOR,
                                null,
                                null,
                                false)
                            .aAutenticacion())))
        .andExpect(status().isForbidden());
  }
}
