package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.EtapaPeriodoRequest;
import com.uniautonoma.docentia.dto.PeriodoAcademicoRequest;
import com.uniautonoma.docentia.entity.Clase;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

class PeriodoAcademicoControllerTest extends PruebaIntegracion {

  private static final LocalDate INICIO = LocalDate.of(2026, 8, 3);
  private static final LocalDate FIN = LocalDate.of(2026, 12, 5);

  @Autowired private PeriodoAcademicoRepository periodoRepository;
  @Autowired private EscenarioPrueba escenario;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void creaUnPeriodoInactivoConSusEtapas() throws Exception {
    EtapaPeriodoRequest etapa =
        new EtapaPeriodoRequest(
            TipoEtapa.RECOLECCION_CALIFICACIONES,
            LocalDate.of(2026, 11, 2),
            LocalDate.of(2026, 11, 13));

    crearPeriodo(new PeriodoAcademicoRequest("2026-2", INICIO, FIN, List.of(etapa)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.esActivo").value(false))
        .andExpect(jsonPath("$.etapas", hasSize(1)))
        .andExpect(jsonPath("$.etapas[0].tipoEtapa").value("RECOLECCION_CALIFICACIONES"))
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
            TipoEtapa.FIRMA_ACTAS, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 20));

    crearPeriodo(new PeriodoAcademicoRequest("2026-2", INICIO, FIN, List.of(etapaTardia)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.mensaje")
                .value("Las fechas de cada etapa deben estar dentro del periodo académico"));
  }

  @Test
  void rechazaUnaEtapaRepetida() throws Exception {
    EtapaPeriodoRequest etapa =
        new EtapaPeriodoRequest(TipoEtapa.FIRMA_ACTAS, INICIO, INICIO.plusDays(5));

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
    PeriodoAcademico nuevo = periodoConMatricula("2026-2");

    mockMvc
        .perform(
            put("/periodos-academicos/{id}/activacion", nuevo.getIdPeriodo())
                .with(comoVicerrector()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.esActivo").value(true));

    mockMvc
        .perform(get("/periodos-academicos/activo").with(comoVicerrector()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("2026-2"));
    assertThat(periodoRepository.findAll().stream().filter(PeriodoAcademico::getEsActivo))
        .hasSize(1);
  }

  @Test
  void activarUnPeriodoSinMatriculasExigeConfirmacion() throws Exception {
    PeriodoAcademico anterior = periodoConMatricula("2026-2");
    anterior.setEsActivo(true);
    periodoRepository.saveAndFlush(anterior);
    PeriodoAcademico vacio =
        periodoRepository.saveAndFlush(new PeriodoAcademico("2027-1", INICIO, FIN));

    mockMvc
        .perform(
            put("/periodos-academicos/{id}/activacion", vacio.getIdPeriodo())
                .with(comoVicerrector()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value(containsString("no tiene matrículas")));
    mockMvc
        .perform(get("/periodos-academicos/activo").with(comoVicerrector()))
        .andExpect(jsonPath("$.nombre").value("2026-2"));

    mockMvc
        .perform(
            put("/periodos-academicos/{id}/activacion", vacio.getIdPeriodo())
                .param("confirmar", "true")
                .with(comoVicerrector()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.esActivo").value(true));
  }

  @Test
  void laActivacionYLaDesactivacionQuedanAuditadas() throws Exception {
    PeriodoAcademico periodo = periodoConMatricula("2026-2");

    mockMvc
        .perform(
            put("/periodos-academicos/{id}/activacion", periodo.getIdPeriodo())
                .with(comoVicerrector()))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            delete("/periodos-academicos/{id}/activacion", periodo.getIdPeriodo())
                .with(comoVicerrector()))
        .andExpect(status().isNoContent());

    assertThat(auditadas("ACTIVACION_PERIODO", periodo.getIdPeriodo())).isEqualTo(1);
    assertThat(auditadas("DESACTIVACION_PERIODO", periodo.getIdPeriodo())).isEqualTo(1);
  }

  @Test
  void unNombreMalEscritoDaUnErrorClaroYNoDejaElSistemaSinPeriodoActivo() throws Exception {
    PeriodoAcademico vigente =
        new PeriodoAcademico("2026-1", LocalDate.of(2026, 2, 2), LocalDate.of(2026, 6, 30));
    vigente.setEsActivo(true);
    periodoRepository.saveAndFlush(vigente);

    crearPeriodo(new PeriodoAcademicoRequest("2026-02", INICIO, FIN, List.of()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.nombre").value(containsString("solo administración")));
    crearComo(
            comoAdministrador(),
            new PeriodoAcademicoRequest("2026 segundo", INICIO, FIN, List.of()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.nombre").value(containsString("AAAA-1 o AAAA-2")));

    mockMvc
        .perform(get("/periodos-academicos/activo").with(comoVicerrector()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("2026-1"));
    crearPeriodo(new PeriodoAcademicoRequest("", INICIO, FIN, List.of()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nombre").value("2026-2"));
  }

  @Test
  void elNombreSeCalculaDesdeLaFechaDeInicio() throws Exception {
    crearPeriodo(
            new PeriodoAcademicoRequest(
                null, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30), List.of()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nombre").value("2027-1"));
    crearPeriodo(
            new PeriodoAcademicoRequest(
                null, LocalDate.of(2027, 7, 1), LocalDate.of(2027, 12, 5), List.of()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nombre").value("2027-2"));
  }

  @Test
  void unPeriodoQueCruzaDeAnioTomaElAnioDeSuInicio() throws Exception {
    crearPeriodo(
            new PeriodoAcademicoRequest(
                null, LocalDate.of(2026, 12, 1), LocalDate.of(2027, 3, 31), List.of()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nombre").value("2026-2"));
    crearPeriodo(
            new PeriodoAcademicoRequest(
                null, LocalDate.of(2027, 6, 30), LocalDate.of(2027, 7, 31), List.of()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nombre").value("2027-1"));
  }

  @Test
  void cambiarLasFechasDeUnPeriodoConEvaluacionesEnviadasConservaSuNombreYSuActivacion()
      throws Exception {
    PeriodoAcademico activo = periodoConMatricula("2026-2");
    activo.setEsActivo(true);
    periodoRepository.saveAndFlush(activo);
    jdbcTemplate.update(
        "insert into participaciones_heteroevaluacion (asignacion_id, estudiante_id, enviada_en)"
            + " select cd.id, m.estudiante_id, now() from clases_docente cd"
            + " join clases c on c.id = cd.clase_id join matriculas m on m.clase_id = c.id"
            + " where c.id_periodo = ?",
        activo.getIdPeriodo());

    mockMvc
        .perform(
            put("/periodos-academicos/{id}", activo.getIdPeriodo())
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(new PeriodoAcademicoRequest(null, INICIO, FIN.plusDays(10), List.of()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("2026-2"))
        .andExpect(jsonPath("$.esActivo").value(true));
  }

  @Test
  void soloAdministracionAjustaElNombreConFormatoYSinRepetir() throws Exception {
    crearComo(comoAdministrador(), new PeriodoAcademicoRequest("2027-1", INICIO, FIN, List.of()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nombre").value("2027-1"));
    crearComo(comoAdministrador(), new PeriodoAcademicoRequest("2027-1", INICIO, FIN, List.of()))
        .andExpect(status().isConflict());
    crearPeriodo(new PeriodoAcademicoRequest("2028-1", INICIO, FIN, List.of()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void unPeriodoParecidoYaExistenteSigueEditandoseSinCambiarSuNombre() throws Exception {
    periodoRepository.save(new PeriodoAcademico("2026-2", INICIO, FIN));
    PeriodoAcademico duplicado =
        periodoRepository.saveAndFlush(new PeriodoAcademico("2026-02", INICIO, FIN));
    EtapaPeriodoRequest etapa =
        new EtapaPeriodoRequest(TipoEtapa.RECOLECCION_CALIFICACIONES, INICIO, INICIO.plusDays(7));

    mockMvc
        .perform(
            put("/periodos-academicos/{id}", duplicado.getIdPeriodo())
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(new PeriodoAcademicoRequest("2026-02", INICIO, FIN, List.of(etapa)))))
        .andExpect(status().isOk());
  }

  @Test
  void respondeNoEncontradoCuandoNoHayPeriodoActivo() throws Exception {
    PeriodoAcademico periodo = new PeriodoAcademico("2026-2", INICIO, FIN);
    periodo.setEsActivo(true);
    periodoRepository.saveAndFlush(periodo);

    mockMvc
        .perform(
            delete("/periodos-academicos/{id}/activacion", periodo.getIdPeriodo())
                .with(comoVicerrector()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/periodos-academicos/activo").with(comoVicerrector()))
        .andExpect(status().isNotFound());
  }

  @Test
  void reprogramaUnaEtapaExistenteYRetiraLasQueYaNoVienen() throws Exception {
    PeriodoAcademico periodo = new PeriodoAcademico("2026-2", INICIO, FIN);
    periodo.programarEtapa(TipoEtapa.RECOLECCION_CALIFICACIONES, INICIO, INICIO.plusDays(10));
    periodo.programarEtapa(TipoEtapa.FIRMA_ACTAS, FIN.minusDays(10), FIN);
    periodoRepository.saveAndFlush(periodo);

    EtapaPeriodoRequest reprogramada =
        new EtapaPeriodoRequest(
            TipoEtapa.RECOLECCION_CALIFICACIONES, INICIO.plusDays(7), INICIO.plusDays(20));

    mockMvc
        .perform(
            put("/periodos-academicos/{id}", periodo.getIdPeriodo())
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        new PeriodoAcademicoRequest("2026-2", INICIO, FIN, List.of(reprogramada)))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.etapas", hasSize(1)))
        .andExpect(jsonPath("$.etapas[0].fechaApertura").value("2026-08-10"));

    periodoRepository.flush();
  }

  private PeriodoAcademico periodoConMatricula(String nombre) {
    Programa programa = escenario.crearPrograma("Software", escenario.crearFacultad("Ingeniería"));
    PeriodoAcademico periodo =
        periodoRepository.saveAndFlush(new PeriodoAcademico(nombre, INICIO, FIN));
    Clase clase =
        escenario.crearClase(
            periodo,
            escenario.crearCurso("ISW-101", "Programación web", programa),
            "A",
            escenario.crearDocente("Docente Uno", programa));
    escenario.matricular(clase, escenario.crearEstudiante("Estudiante Uno", programa));
    return periodo;
  }

  private long auditadas(String accion, Long idPeriodo) {
    return jdbcTemplate.queryForObject(
        "select count(*) from auditoria where accion = ? and entidad = 'PERIODO'"
            + " and entidad_id = ? and resultado = 'EXITO'",
        Long.class,
        accion,
        idPeriodo);
  }

  private ResultActions crearPeriodo(PeriodoAcademicoRequest solicitud) throws Exception {
    return crearComo(comoVicerrector(), solicitud);
  }

  private ResultActions crearComo(
      org.springframework.test.web.servlet.request.RequestPostProcessor rol,
      PeriodoAcademicoRequest solicitud)
      throws Exception {
    return mockMvc.perform(
        post("/periodos-academicos")
            .with(rol)
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(solicitud)));
  }
}
