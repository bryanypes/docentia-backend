package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.EtapaPeriodoRequest;
import com.uniautonoma.docentia.dto.PeriodoAcademicoRequest;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class PeriodoAcademicoControllerTest extends PruebaIntegracion {

  private static final LocalDate INICIO = LocalDate.of(2026, 8, 3);
  private static final LocalDate FIN = LocalDate.of(2026, 12, 5);

  @Autowired private PeriodoAcademicoRepository periodoRepository;

  @Test
  void creaUnPeriodoInactivoConSusEtapas() throws Exception {
    EtapaPeriodoRequest etapa =
        new EtapaPeriodoRequest(
            TipoEtapa.CALIFICACION_COORDINACION,
            LocalDate.of(2026, 11, 2),
            LocalDate.of(2026, 11, 13));

    crearPeriodo(new PeriodoAcademicoRequest("2026-2", INICIO, FIN, List.of(etapa)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.esActivo").value(false))
        .andExpect(jsonPath("$.etapas", hasSize(1)))
        .andExpect(jsonPath("$.etapas[0].tipoEtapa").value("CALIFICACION_COORDINACION"))
        .andExpect(jsonPath("$.etapas[0].fechaApertura").value("2026-11-02"));
  }

  @Test
  void rechazaUnPeriodoQueTerminaAntesDeEmpezar() throws Exception {
    crearPeriodo(new PeriodoAcademicoRequest("2026-2", FIN, INICIO, List.of()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rechazaUnaEtapaFueraDelPeriodo() throws Exception {
    EtapaPeriodoRequest etapaTardia =
        new EtapaPeriodoRequest(
            TipoEtapa.RETROALIMENTACION, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 20));

    crearPeriodo(new PeriodoAcademicoRequest("2026-2", INICIO, FIN, List.of(etapaTardia)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.mensaje")
                .value("Las fechas de cada etapa deben estar dentro del periodo académico"));
  }

  @Test
  void rechazaUnaEtapaRepetida() throws Exception {
    EtapaPeriodoRequest etapa =
        new EtapaPeriodoRequest(TipoEtapa.RETROALIMENTACION, INICIO, INICIO.plusDays(5));

    crearPeriodo(new PeriodoAcademicoRequest("2026-2", INICIO, FIN, List.of(etapa, etapa)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rechazaUnNombreDePeriodoRepetido() throws Exception {
    periodoRepository.save(new PeriodoAcademico("2026-2", INICIO, FIN));

    crearPeriodo(new PeriodoAcademicoRequest("2026-2", INICIO, FIN, List.of()))
        .andExpect(status().isConflict());
  }

  @Test
  void alActivarUnPeriodoElAnteriorDejaDeEstarActivo() throws Exception {
    PeriodoAcademico anterior = new PeriodoAcademico("2026-1", LocalDate.of(2026, 2, 2), INICIO);
    anterior.setEsActivo(true);
    periodoRepository.saveAndFlush(anterior);
    PeriodoAcademico nuevo =
        periodoRepository.saveAndFlush(new PeriodoAcademico("2026-2", INICIO, FIN));

    mockMvc
        .perform(put("/periodos-academicos/{id}/activacion", nuevo.getIdPeriodo()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.esActivo").value(true));

    mockMvc
        .perform(get("/periodos-academicos/activo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("2026-2"));
    assertThat(periodoRepository.findAll().stream().filter(PeriodoAcademico::getEsActivo))
        .hasSize(1);
  }

  @Test
  void respondeNoEncontradoCuandoNoHayPeriodoActivo() throws Exception {
    PeriodoAcademico periodo = new PeriodoAcademico("2026-2", INICIO, FIN);
    periodo.setEsActivo(true);
    periodoRepository.saveAndFlush(periodo);

    mockMvc
        .perform(delete("/periodos-academicos/{id}/activacion", periodo.getIdPeriodo()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/periodos-academicos/activo")).andExpect(status().isNotFound());
  }

  @Test
  void reprogramaUnaEtapaExistenteYRetiraLasQueYaNoVienen() throws Exception {
    PeriodoAcademico periodo = new PeriodoAcademico("2026-2", INICIO, FIN);
    periodo.programarEtapa(TipoEtapa.CALIFICACION_COORDINACION, INICIO, INICIO.plusDays(10));
    periodo.programarEtapa(TipoEtapa.RETROALIMENTACION, FIN.minusDays(10), FIN);
    periodoRepository.saveAndFlush(periodo);

    EtapaPeriodoRequest reprogramada =
        new EtapaPeriodoRequest(
            TipoEtapa.CALIFICACION_COORDINACION, INICIO.plusDays(7), INICIO.plusDays(20));

    mockMvc
        .perform(
            put("/periodos-academicos/{id}", periodo.getIdPeriodo())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        new PeriodoAcademicoRequest("2026-2", INICIO, FIN, List.of(reprogramada)))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.etapas", hasSize(1)))
        .andExpect(jsonPath("$.etapas[0].fechaApertura").value("2026-08-10"));

    periodoRepository.flush();
  }

  private ResultActions crearPeriodo(PeriodoAcademicoRequest solicitud) throws Exception {
    return mockMvc.perform(
        post("/periodos-academicos")
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(solicitud)));
  }
}
