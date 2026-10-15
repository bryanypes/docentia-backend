package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.CalificacionRequest;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Las calificaciones solo entran mientras la recolección del periodo activo está abierta. */
class VentanaEtapaCalificacionTest extends PruebaIntegracion {

  private static final LocalDate HOY = BaseDatosPruebaConfig.HOY;

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CalificacionRepository calificacionRepository;

  private Usuario docente;
  private Usuario coordinador;
  private Usuario vicerrector;

  @BeforeEach
  void crearDocenteYEvaluadores() {
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    docente =
        escenario.crearDocente(
            "Julian Hoyos", escenario.crearPrograma("Ingeniería de Software", ingenieria));
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    vicerrector = escenario.crearVicerrector("Elena Munoz");
  }

  @Test
  void conLaRecoleccionAbiertaSePuedeCalificar() throws Exception {
    escenario.crearPeriodoActivoConRecoleccion("2026-2", HOY, HOY);

    calificar(coordinador, "coordinacion").andExpect(status().isOk());
    calificar(vicerrector, "vicerrectoria").andExpect(status().isOk());
  }

  @Test
  void antesDeLaRecoleccionRespondeConflictoConLaEtapaYSusFechas() throws Exception {
    escenario.crearPeriodoActivoConRecoleccion("2026-2", HOY.plusDays(1), HOY.plusDays(10));

    calificar(coordinador, "coordinacion")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.etapa").value("RECOLECCION_CALIFICACIONES"))
        .andExpect(jsonPath("$.nombreEtapa").value("Recolección de calificaciones"))
        .andExpect(jsonPath("$.fechaApertura").value("2026-10-21"))
        .andExpect(jsonPath("$.fechaCierre").value("2026-10-30"))
        .andExpect(jsonPath("$.fechaActual").value("2026-10-20"))
        .andExpect(
            jsonPath("$.mensaje")
                .value(
                    "La etapa Recolección de calificaciones del periodo 2026-2 está abierta del"
                        + " 2026-10-21 al 2026-10-30; hoy es 2026-10-20"));
    assertThat(calificacionRepository.count()).isZero();
  }

  @Test
  void despuesDeLaRecoleccionTampocoCalificaElVicerrector() throws Exception {
    escenario.crearPeriodoActivoConRecoleccion("2026-2", HOY.minusDays(10), HOY.minusDays(1));

    calificar(vicerrector, "vicerrectoria").andExpect(status().isConflict());
    assertThat(calificacionRepository.count()).isZero();
  }

  @Test
  void unDocenteFueraDeAlcanceSigueRespondiendoNoEncontradoAntesQueLaEtapa() throws Exception {
    escenario.crearPeriodoActivoConRecoleccion("2026-2", HOY.plusDays(1), HOY.plusDays(10));
    Usuario coordinadorDerecho =
        escenario.crearCoordinador("Paola Sarria", escenario.crearFacultad("Derecho"));

    calificar(coordinadorDerecho, "coordinacion").andExpect(status().isNotFound());
  }

  @Test
  void lasVentanasDelPeriodoActivoDicenQueEtapaEstaAbiertaHoy() throws Exception {
    escenario.crearPeriodoActivoConRecoleccion("2026-2", HOY.minusDays(5), HOY.plusDays(5));

    mockMvc
        .perform(get("/periodos-academicos/activo/ventanas").with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].etapa").value("RECOLECCION_CALIFICACIONES"))
        .andExpect(jsonPath("$[0].abierta").value(true))
        .andExpect(jsonPath("$[0].fechaApertura").value("2026-10-15"))
        .andExpect(jsonPath("$[3].etapa").value("FIRMA_ACTAS"))
        .andExpect(jsonPath("$[3].abierta").value(false))
        .andExpect(jsonPath("$[3].fechaApertura").doesNotExist());
  }

  @Test
  void sinPeriodoActivoLasVentanasRespondenConflicto() throws Exception {
    mockMvc
        .perform(get("/periodos-academicos/activo/ventanas").with(comoVicerrector()))
        .andExpect(status().isConflict());
  }

  private ResultActions calificar(Usuario evaluador, String fuente) throws Exception {
    BigDecimal nota = new BigDecimal("4.0");
    return mockMvc.perform(
        put("/docentes/{id}/calificaciones/" + fuente, docente.getIdUsuario())
            .with(como(evaluador))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new CalificacionRequest(nota, nota, nota, nota, nota, null))));
  }
}
