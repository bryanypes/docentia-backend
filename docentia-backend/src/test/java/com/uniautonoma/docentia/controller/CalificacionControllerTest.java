package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.CalificacionRequest;
import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class CalificacionControllerTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CalificacionRepository calificacionRepository;

  private Usuario coordinadorIngenieria;
  private Usuario docenteIngenieria;
  private Usuario docenteDerecho;

  @BeforeEach
  void crearEscenario() {
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    Facultad derecho = escenario.crearFacultad("Derecho");
    coordinadorIngenieria = escenario.crearCoordinador("Andres Ordonez", ingenieria);
    docenteIngenieria =
        escenario.crearDocente(
            "Julian Hoyos", escenario.crearPrograma("Ingeniería de Software", ingenieria));
    docenteDerecho =
        escenario.crearDocente("Luisa Mosquera", escenario.crearPrograma("Derecho", derecho));
  }

  @Test
  void elCoordinadorCalificaLosCincoAspectosDeUnDocenteDeSuFacultad() throws Exception {
    escenario.crearPeriodoActivo("2026-2");

    calificarComoCoordinacion(
            docenteIngenieria, notas("4.5", "4.0", "3.8", "5.0", "1.0", "Buen semestre"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fuente").value("COORDINACION"))
        .andExpect(jsonPath("$.nombrePeriodo").value("2026-2"))
        .andExpect(jsonPath("$.dominioDisciplinar").value(4.5))
        .andExpect(jsonPath("$.compromisoInstitucional").value(1.0))
        .andExpect(jsonPath("$.observacion").value("Buen semestre"));

    Calificacion guardada = calificacionRepository.findAll().getFirst();
    assertThat(guardada.getFuente()).isEqualTo(FuenteEvaluacion.COORDINACION);
    assertThat(guardada.getEvaluador().getIdUsuario())
        .isEqualTo(coordinadorIngenieria.getIdUsuario());
  }

  @Test
  void laObservacionEsOpcional() throws Exception {
    escenario.crearPeriodoActivo("2026-2");

    calificarComoCoordinacion(docenteIngenieria, notas("4.5", "4.0", "3.8", "5.0", "4.2", "  "))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.observacion").doesNotExist());
  }

  @Test
  void volverACalificarReemplazaLaCalificacionAnterior() throws Exception {
    escenario.crearPeriodoActivo("2026-2");
    calificarComoCoordinacion(docenteIngenieria, notas("3.0", "3.0", "3.0", "3.0", "3.0", null));

    calificarComoCoordinacion(docenteIngenieria, notas("4.0", "4.0", "4.0", "4.0", "4.0", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dominioDisciplinar").value(4.0));

    assertThat(calificacionRepository.count()).isEqualTo(1);
  }

  @Test
  void rechazaNotasFueraDelRangoDeUnoACinco() throws Exception {
    escenario.crearPeriodoActivo("2026-2");

    calificarComoCoordinacion(docenteIngenieria, notas("0.9", "5.1", "3.0", "3.0", "3.0", null))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.dominioDisciplinar").exists())
        .andExpect(jsonPath("$.errores.planeacionAcademica").exists())
        .andExpect(jsonPath("$.errores.metodologiaEnsenanza").doesNotExist());

    assertThat(calificacionRepository.count()).isZero();
  }

  @Test
  void rechazaNotasConMasDeUnDecimalOSinValor() throws Exception {
    escenario.crearPeriodoActivo("2026-2");
    CalificacionRequest solicitud =
        new CalificacionRequest(
            new BigDecimal("4.25"),
            null,
            new BigDecimal("3.0"),
            new BigDecimal("3.0"),
            new BigDecimal("3.0"),
            null);

    calificarComoCoordinacion(docenteIngenieria, solicitud)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.dominioDisciplinar").exists())
        .andExpect(jsonPath("$.errores.planeacionAcademica").exists());
  }

  @Test
  void unDocenteDeOtraFacultadNoSePuedeCalificar() throws Exception {
    escenario.crearPeriodoActivo("2026-2");

    calificarComoCoordinacion(docenteDerecho, notas("4.0", "4.0", "4.0", "4.0", "4.0", null))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("Docente no encontrado"));

    assertThat(calificacionRepository.count()).isZero();
  }

  @Test
  void sinPeriodoActivoNoSePuedeCalificar() throws Exception {
    calificarComoCoordinacion(docenteIngenieria, notas("4.0", "4.0", "4.0", "4.0", "4.0", null))
        .andExpect(status().isConflict());
  }

  @Test
  void laConsultaDevuelveLaCalificacionGuardadaOSinContenidoSiNoHay() throws Exception {
    escenario.crearPeriodoActivo("2026-2");
    String ruta = "/docentes/{id}/calificaciones/coordinacion";

    mockMvc
        .perform(get(ruta, docenteIngenieria.getIdUsuario()).with(como(coordinadorIngenieria)))
        .andExpect(status().isNoContent());

    calificarComoCoordinacion(docenteIngenieria, notas("4.0", "4.0", "4.0", "4.0", "4.0", null));

    mockMvc
        .perform(get(ruta, docenteIngenieria.getIdUsuario()).with(como(coordinadorIngenieria)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.evaluacionAprendizaje").value(4.0));
    mockMvc
        .perform(get(ruta, docenteDerecho.getIdUsuario()).with(como(coordinadorIngenieria)))
        .andExpect(status().isNotFound());
  }

  @Test
  void soloElCoordinadorUsaElEndpointDeCoordinacion() throws Exception {
    escenario.crearPeriodoActivo("2026-2");
    String cuerpo = aJson(notas("4.0", "4.0", "4.0", "4.0", "4.0", null));

    for (var otroRol : List.of(comoVicerrector(), como(docenteIngenieria))) {
      mockMvc
          .perform(
              put("/docentes/{id}/calificaciones/coordinacion", docenteIngenieria.getIdUsuario())
                  .with(otroRol)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(cuerpo))
          .andExpect(status().isForbidden());
    }
  }

  private ResultActions calificarComoCoordinacion(Usuario docente, CalificacionRequest solicitud)
      throws Exception {
    return mockMvc.perform(
        put("/docentes/{id}/calificaciones/coordinacion", docente.getIdUsuario())
            .with(como(coordinadorIngenieria))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(solicitud)));
  }

  private CalificacionRequest notas(
      String dominio,
      String planeacion,
      String metodologia,
      String evaluacion,
      String compromiso,
      String observacion) {
    return new CalificacionRequest(
        new BigDecimal(dominio),
        new BigDecimal(planeacion),
        new BigDecimal(metodologia),
        new BigDecimal(evaluacion),
        new BigDecimal(compromiso),
        observacion);
  }
}
