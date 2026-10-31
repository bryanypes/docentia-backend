package com.uniautonoma.docentia.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.CumplimientoRequest;
import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CompromisoRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Res. 074, paso 7: en el periodo N se revisan los compromisos del periodo N-1. */
class PlanSeguimientoTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CompromisoRepository compromisoRepository;

  private Facultad ingenieria;
  private Usuario coordinador;
  private Usuario docente;
  private Compromiso vencido;
  private Compromiso pendiente;

  @BeforeEach
  void crearEscenario() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    docente =
        escenario.crearDocente("Julian Hoyos", escenario.crearPrograma("Software", ingenieria));
    PeriodoAcademico anterior = escenario.crearPeriodoCerrado("2026-1", LocalDate.of(2026, 2, 2));
    escenario.crearPeriodoActivo("2026-2");
    vencido =
        compromisoRepository.save(
            new Compromiso(
                docente, anterior, "Entregar el plan", BaseDatosPruebaConfig.HOY.minusDays(1)));
    pendiente =
        compromisoRepository.save(
            new Compromiso(
                docente, anterior, "Publicar notas", BaseDatosPruebaConfig.HOY.plusDays(10)));
  }

  @Test
  void muestraLosCompromisosDelPeriodoAnteriorConSuEstado() throws Exception {
    mockMvc
        .perform(
            get("/docentes/{id}/plan-seguimiento", docente.getIdUsuario()).with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodoEvaluado.nombre").value("2026-1"))
        .andExpect(jsonPath("$.compromisos[*].estado", contains("VENCIDO", "PENDIENTE")));
  }

  @Test
  void marcarCumplidoDejaFechaYAutor() throws Exception {
    cumplir(pendiente.getIdCompromiso(), "Verificado en el sistema")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.compromisos[1].estado").value("CUMPLIDO"))
        .andExpect(jsonPath("$.compromisos[1].observacion").value("Verificado en el sistema"))
        .andExpect(jsonPath("$.compromisos[1].historial", hasSize(1)))
        .andExpect(jsonPath("$.compromisos[1].historial[0].nombreUsuario").value("Marta Ruiz"))
        .andExpect(jsonPath("$.compromisos[1].historial[0].fecha").exists());
  }

  @Test
  void laObservacionEsObligatoria() throws Exception {
    cumplir(vencido.getIdCompromiso(), " ")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.observacion").exists());
  }

  @Test
  void elDocenteConsultaElSuyoPeroNoLoMarca() throws Exception {
    mockMvc
        .perform(get("/plan-seguimiento/propio").with(como(docente)))
        .andExpect(jsonPath("$.compromisos", hasSize(2)));
    mockMvc
        .perform(
            put("/compromisos/{id}/cumplimiento", vencido.getIdCompromiso())
                .with(como(docente))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new CumplimientoRequest("Listo"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void respetaElAlcancePorFacultad() throws Exception {
    Usuario otro = escenario.crearCoordinador("Paola Sarria", escenario.crearFacultad("Derecho"));

    mockMvc
        .perform(get("/docentes/{id}/plan-seguimiento", docente.getIdUsuario()).with(como(otro)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            put("/compromisos/{id}/cumplimiento", vencido.getIdCompromiso())
                .with(como(otro))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new CumplimientoRequest("Listo"))))
        .andExpect(status().isNotFound());
  }

  private ResultActions cumplir(Long idCompromiso, String observacion) throws Exception {
    return mockMvc.perform(
        put("/compromisos/{id}/cumplimiento", idCompromiso)
            .with(como(coordinador))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new CumplimientoRequest(observacion))));
  }
}
