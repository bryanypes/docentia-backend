package com.uniautonoma.docentia.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.CalificacionRequest;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Usuario;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Res. 074: el 20 % lo aporta la autoevaluación del docente, no la vicerrectoría. */
class AutoevaluacionTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;

  private Facultad ingenieria;
  private Usuario docente;
  private PeriodoAcademico periodo;

  @BeforeEach
  void crearEscenario() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    docente =
        escenario.crearDocente("Julian Hoyos", escenario.crearPrograma("Software", ingenieria));
    periodo =
        escenario.crearPeriodoActivoConRecoleccion(
            "2026-2", BaseDatosPruebaConfig.HOY, BaseDatosPruebaConfig.HOY);
  }

  @Test
  void elDocenteRegistraYConsultaSuAutoevaluacion() throws Exception {
    mockMvc.perform(get("/autoevaluacion").with(como(docente))).andExpect(status().isNoContent());

    registrar(como(docente), "4.8")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fuente").value("AUTOEVALUACION"));

    mockMvc
        .perform(get("/autoevaluacion").with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dominioDisciplinar").value(4.8));
  }

  @Test
  void conLasTresFuentesAplicaLaFormulaDeLaResolucion() throws Exception {
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.ESTUDIANTES, "4.5");
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.COORDINACION, "4.0");
    registrar(como(docente), "4.8").andExpect(status().isOk());

    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(jsonPath("$.promedio.estado").value("COMPLETO"))
        .andExpect(jsonPath("$.promedio.valor").value(4.41))
        .andExpect(jsonPath("$.promedio.categoria.codigo").value("ALTO"));
  }

  @Test
  void soloElDocenteSeAutoevalua() throws Exception {
    registrar(comoVicerrector(), "4.0").andExpect(status().isForbidden());
    registrar(comoCoordinador(ingenieria.getIdFacultad()), "4.0").andExpect(status().isForbidden());
    mockMvc
        .perform(
            put("/docentes/{id}/calificaciones/autoevaluacion", docente.getIdUsuario())
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(notas("4.0"))))
        .andExpect(status().isNotFound());
  }

  @Test
  void aplicaLasValidacionesDeRango() throws Exception {
    registrar(como(docente), "5.5").andExpect(status().isBadRequest());
  }

  private ResultActions registrar(RequestPostProcessor sesion, String nota) throws Exception {
    return mockMvc.perform(
        put("/autoevaluacion")
            .with(sesion)
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(notas(nota))));
  }

  private CalificacionRequest notas(String nota) {
    BigDecimal valor = new BigDecimal(nota);
    return new CalificacionRequest(valor, valor, valor, valor, valor, null);
  }
}
