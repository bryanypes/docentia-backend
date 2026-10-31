package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.CalificacionRequest;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class CalificacionVicerrectoriaTest extends PruebaIntegracion {

  private static final String RUTA_VICERRECTORIA = "/docentes/{id}/calificaciones/vicerrectoria";
  private static final String RUTA_COORDINACION = "/docentes/{id}/calificaciones/coordinacion";

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CalificacionRepository calificacionRepository;

  private PeriodoAcademico periodo;
  private Usuario vicerrector;
  private Usuario coordinadorIngenieria;
  private Usuario docenteIngenieria;
  private Usuario docenteDerecho;

  @BeforeEach
  void crearEscenario() {
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    Facultad derecho = escenario.crearFacultad("Derecho");
    vicerrector = escenario.crearVicerrector("Elena Munoz");
    coordinadorIngenieria = escenario.crearCoordinador("Andres Ordonez", ingenieria);
    docenteIngenieria =
        escenario.crearDocente(
            "Julian Hoyos", escenario.crearPrograma("Ingeniería de Software", ingenieria));
    docenteDerecho =
        escenario.crearDocente("Luisa Mosquera", escenario.crearPrograma("Derecho", derecho));
    periodo = escenario.crearPeriodoActivo("2026-2");
  }

  @Test
  void elVicerrectorCalificaDocentesDeCualquierFacultad() throws Exception {
    calificar(RUTA_VICERRECTORIA, docenteIngenieria, como(vicerrector), "4.0")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fuente").value("VICERRECTORIA"));
    calificar(RUTA_VICERRECTORIA, docenteDerecho, como(vicerrector), "3.5")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.metodologiaEnsenanza").value(3.5));

    assertThat(calificacionRepository.count()).isEqualTo(2);
  }

  @Test
  void laCalificacionDeVicerrectoriaNoPisaLaDeCoordinacion() throws Exception {
    calificar(RUTA_COORDINACION, docenteIngenieria, como(coordinadorIngenieria), "3.0");
    calificar(RUTA_VICERRECTORIA, docenteIngenieria, como(vicerrector), "5.0");

    assertThat(notaGuardada(FuenteEvaluacion.COORDINACION)).isEqualByComparingTo("3.0");
    assertThat(notaGuardada(FuenteEvaluacion.VICERRECTORIA)).isEqualByComparingTo("5.0");
    mockMvc
        .perform(get(RUTA_VICERRECTORIA, docenteIngenieria.getIdUsuario()).with(como(vicerrector)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dominioDisciplinar").value(5.0));
  }

  @Test
  void aplicaLasMismasValidacionesDeRango() throws Exception {
    calificar(RUTA_VICERRECTORIA, docenteDerecho, como(vicerrector), "5.5")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.dominioDisciplinar").exists());
  }

  @Test
  void respondeNoEncontradoSiElDocenteNoExiste() throws Exception {
    mockMvc
        .perform(
            put(RUTA_VICERRECTORIA, 999_999)
                .with(como(vicerrector))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(notasUniformes("4.0"))))
        .andExpect(status().isNotFound());
  }

  @Test
  void elCoordinadorNoPuedeCalificarANombreDeVicerrectoria() throws Exception {
    calificar(RUTA_VICERRECTORIA, docenteIngenieria, como(coordinadorIngenieria), "4.0")
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            get(RUTA_VICERRECTORIA, docenteIngenieria.getIdUsuario())
                .with(como(coordinadorIngenieria)))
        .andExpect(status().isForbidden());
  }

  private ResultActions calificar(
      String ruta, Usuario docente, RequestPostProcessor sesion, String nota) throws Exception {
    return mockMvc.perform(
        put(ruta, docente.getIdUsuario())
            .with(sesion)
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(notasUniformes(nota))));
  }

  private CalificacionRequest notasUniformes(String nota) {
    BigDecimal valor = new BigDecimal(nota);
    return new CalificacionRequest(valor, valor, valor, valor, valor, null);
  }

  private BigDecimal notaGuardada(FuenteEvaluacion fuente) {
    return calificacionRepository
        .findByDocenteIdUsuarioAndPeriodoIdPeriodoAndFuente(
            docenteIngenieria.getIdUsuario(), periodo.getIdPeriodo(), fuente)
        .orElseThrow()
        .getNotas()
        .dominioDisciplinar();
  }
}
