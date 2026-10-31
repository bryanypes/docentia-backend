package com.uniautonoma.docentia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.EstadoPromedio;
import com.uniautonoma.docentia.dto.FranjaRequest;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.PromedioDocenteRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** 1.7, I11 y calendario: lecturas agregadas o informativas dentro del alcance de cada rol. */
class AccionesYPanelTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;
  @Autowired private PromedioDocenteRepository promedioRepository;

  private Facultad ingenieria;
  private PeriodoAcademico periodo;
  private Usuario coordinador;
  private Usuario vicerrector;
  private Usuario docente;

  @BeforeEach
  void crearEscenario() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    Facultad derecho = escenario.crearFacultad("Derecho");
    Programa software = escenario.crearPrograma("Software", ingenieria);
    Programa leyes = escenario.crearPrograma("Leyes", derecho);
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    vicerrector = escenario.crearVicerrector("Ana Vice");
    periodo = escenario.crearPeriodoActivo("2026-2");
    docente = escenario.crearDocente("Ramiro Paz", software);
    guardar(docente, "1.90", "MUY_BAJO", EstadoPromedio.COMPLETO);
    guardar(
        escenario.crearDocente("Nora Munoz", software), "2.85", "BAJO", EstadoPromedio.COMPLETO);
    guardar(
        escenario.crearDocente("Camilo Vidal", software),
        "4.75",
        "MUY_ALTO",
        EstadoPromedio.PARCIAL);
    escenario.crearDocente("Sin Nota", software);
    guardar(
        escenario.crearDocente("Otra Facultad", leyes), "4.00", "ALTO", EstadoPromedio.COMPLETO);
  }

  private void guardar(Usuario usuario, String valor, String categoria, EstadoPromedio estado) {
    promedioRepository.guardar(
        usuario.getIdUsuario(),
        periodo.getIdPeriodo(),
        new BigDecimal(valor),
        estado.name(),
        categoria,
        estado == EstadoPromedio.PARCIAL ? "AUTOEVALUACION" : null);
  }

  @Test
  void elCoordinadorVeSoloSuFacultadYMuyBajoEsInformativo() throws Exception {
    mockMvc
        .perform(get("/acciones-sugeridas").with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].nombreDocente").value("Ramiro Paz"))
        .andExpect(jsonPath("$[0].categoria.codigo").value("MUY_BAJO"))
        .andExpect(
            jsonPath("$[0].accion")
                .value(
                    "Alerta informativa para Vicerrectoría — la institución evalúa el proceso"
                        + " conforme a su normativa"))
        .andExpect(jsonPath("$[2].estadoPromedio").value("PARCIAL"))
        .andExpect(content().string(not(containsString("esvincula"))));
  }

  @Test
  void elVicerrectorVeTodaLaInstitucionYElDocenteRecibe403() throws Exception {
    mockMvc
        .perform(
            get("/acciones-sugeridas")
                .param("periodo", periodo.getIdPeriodo().toString())
                .with(como(vicerrector)))
        .andExpect(jsonPath("$", hasSize(4)));
    mockMvc
        .perform(get("/acciones-sugeridas").with(como(docente)))
        .andExpect(status().isForbidden());
  }

  @Test
  void elPanelAgregaPorProgramaSinIdentificarDocentes() throws Exception {
    mockMvc
        .perform(get("/kpis/panel").with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.programas", hasSize(1)))
        .andExpect(jsonPath("$.programas[0].programa").value("Software"))
        .andExpect(jsonPath("$.programas[0].docentes").value(4))
        .andExpect(jsonPath("$.programas[0].evaluados").value(2))
        .andExpect(jsonPath("$.programas[0].cobertura").value(50.0))
        .andExpect(jsonPath("$.programas[0].promedio").value(2.38))
        .andExpect(jsonPath("$.programas[0].distribucion[3].codigo").value("BAJO"))
        .andExpect(jsonPath("$.programas[0].distribucion[3].cantidad").value(1))
        .andExpect(jsonPath("$.programas[0].distribucion[3].porcentaje").value(50.0))
        .andExpect(jsonPath("$.programas[0].distribucion[0].cantidad").value(0))
        .andExpect(content().string(not(containsString("Ramiro"))))
        .andExpect(content().string(not(containsString("idDocente"))));
  }

  @Test
  void elTotalDelVicerrectorSumaLosProgramas() throws Exception {
    mockMvc
        .perform(get("/kpis/panel").with(como(vicerrector)))
        .andExpect(jsonPath("$.programas", hasSize(2)))
        .andExpect(jsonPath("$.total.docentes").value(5))
        .andExpect(jsonPath("$.total.evaluados").value(3))
        .andExpect(jsonPath("$.total.promedio").value(2.92))
        .andExpect(jsonPath("$.total.distribucion[1].cantidad").value(1));
  }

  @Test
  void elDocenteNoVeElPanel() throws Exception {
    mockMvc.perform(get("/kpis/panel").with(como(docente))).andExpect(status().isForbidden());
  }

  @Test
  void elCalendarioDaLaEtapaVigenteYLaSemana() throws Exception {
    escenario.abrirEtapa(
        periodo,
        TipoEtapa.REVISION_HALLAZGOS,
        BaseDatosPruebaConfig.HOY,
        BaseDatosPruebaConfig.HOY.plusDays(5));

    mockMvc
        .perform(get("/periodos-academicos/activo/calendario").with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodo.nombre").value("2026-2"))
        .andExpect(jsonPath("$.etapaVigente.etapa").value("REVISION_HALLAZGOS"))
        .andExpect(jsonPath("$.etapaVigente.semanasResolucion").value("Semana 15 (consolidación)"))
        .andExpect(jsonPath("$.etapas", hasSize(4)))
        .andExpect(jsonPath("$.seguimiento").value("Seguimiento del plan: período siguiente"));
  }

  @Test
  void laFranjaDeLas2330RecibeUnMensajeClaro() throws Exception {
    escenario.abrirEtapa(
        periodo,
        TipoEtapa.AGENDA_CITAS,
        BaseDatosPruebaConfig.HOY,
        BaseDatosPruebaConfig.HOY.plusDays(5));

    mockMvc
        .perform(
            post("/franjas")
                .with(como(coordinador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        new FranjaRequest(
                            BaseDatosPruebaConfig.HOY.plusDays(1),
                            LocalTime.of(23, 30),
                            LocalTime.MIDNIGHT))))
        .andExpect(
            content().string(containsString("La última franja del día empieza a las 23:00")));
  }
}
