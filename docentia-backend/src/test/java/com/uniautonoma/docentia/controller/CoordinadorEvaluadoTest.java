package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.FirmaActaRequest;
import com.uniautonoma.docentia.dto.NuevaActaRequest;
import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CitaRepository;
import com.uniautonoma.docentia.repository.FranjaHorariaRepository;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * El coordinador también es docente evaluado: la vicerrectoría lo coevalúa, le abre franjas y firma
 * su acta en el lugar de la coordinación.
 */
class CoordinadorEvaluadoTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CitaRepository citaRepository;
  @Autowired private FranjaHorariaRepository franjaRepository;

  private Usuario coordinador;
  private Usuario otroCoordinador;
  private Usuario docente;
  private Usuario vicerrector;
  private PeriodoAcademico periodo;

  @BeforeEach
  void crearEscenario() {
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    otroCoordinador = escenario.crearCoordinador("Luis Pardo", escenario.crearFacultad("Salud"));
    vicerrector = escenario.crearVicerrector("Elena Munoz");
    docente = escenario.crearDocente("Nora Munoz", escenario.crearPrograma("Software", ingenieria));
    periodo =
        escenario.abrirEtapa(
            escenario.crearPeriodoActivo("2026-2"),
            TipoEtapa.FIRMA_ACTAS,
            BaseDatosPruebaConfig.HOY,
            BaseDatosPruebaConfig.HOY);
  }

  @Test
  void laVicerrectoriaVeALosCoordinadoresComoDocentesYLaCoordinacionNo() throws Exception {
    mockMvc
        .perform(get("/docentes").with(como(vicerrector)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(
            jsonPath("$[?(@.esCoordinador == true)].nombreCompleto")
                .value(containsInAnyOrder("Marta Ruiz", "Luis Pardo")));
    mockMvc
        .perform(get("/docentes/{id}", coordinador.getIdUsuario()).with(como(vicerrector)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombreFacultad").value("Ingeniería"))
        .andExpect(jsonPath("$.idPrograma").doesNotExist());

    mockMvc
        .perform(get("/docentes").with(como(coordinador)))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].nombreCompleto").value("Nora Munoz"));
    mockMvc
        .perform(get("/docentes/{id}", otroCoordinador.getIdUsuario()).with(como(coordinador)))
        .andExpect(status().isNotFound());
  }

  @Test
  void elCoordinadorReservaSoloFranjasDeLaVicerrectoria() throws Exception {
    periodo =
        escenario.abrirEtapa(
            periodo,
            TipoEtapa.AGENDA_CITAS,
            BaseDatosPruebaConfig.HOY,
            BaseDatosPruebaConfig.HOY.plusDays(5));
    FranjaHoraria deVicerrectoria = franja(vicerrector);
    FranjaHoraria deCoordinacion = franja(coordinador);

    mockMvc
        .perform(get("/franjas/disponibles").with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].idFranja").value(deVicerrectoria.getIdFranja()));
    mockMvc
        .perform(get("/franjas/disponibles").with(como(docente)))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].idFranja").value(deCoordinacion.getIdFranja()));
    mockMvc
        .perform(post("/franjas/{id}/reserva", deVicerrectoria.getIdFranja()).with(como(docente)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/franjas/{id}/reserva", deVicerrectoria.getIdFranja()).with(como(coordinador)))
        .andExpect(status().isCreated());
  }

  @Test
  void laVicerrectoriaFirmaElActaDelCoordinadorEnLugarDeLaCoordinacion() throws Exception {
    for (FuenteEvaluacion fuente : FuenteEvaluacion.values()) {
      escenario.registrarCalificacion(coordinador, periodo, fuente, "4.5");
    }
    citaRepository.saveAndFlush(new Cita(franja(vicerrector), coordinador));

    crearActa(otroCoordinador, coordinador).andExpect(status().isNotFound());
    Long acta = idDe(crearActa(vicerrector, coordinador).andExpect(status().isCreated()));

    mockMvc
        .perform(get("/actas").with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].idActa").value(acta));
    firmar(acta, coordinador)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firmas[0].rol").value("DOCENTE"));
    firmar(acta, vicerrector)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("FIRMADA"))
        .andExpect(jsonPath("$.firmas[*].rol").value(containsInAnyOrder("DOCENTE", "VICERRECTOR")));

    byte[] pdf =
        mockMvc
            .perform(get("/actas/{id}/pdf", acta).with(como(coordinador)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    String texto = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1);
    assertThat(texto.replaceAll("\\s+", " "))
        .contains("Firma vicerrectoría")
        .contains("y la vicerrectoría firmaron esta acta")
        .doesNotContain("Firma coordinación")
        .doesNotContain("la coordinación firmaron");
  }

  @Test
  void elCoordinadorVeSuPromedioConLaNotaDeLaVicerrectoria() throws Exception {
    escenario.registrarCalificacion(coordinador, periodo, FuenteEvaluacion.ESTUDIANTES, "4.0");
    escenario.registrarCalificacion(coordinador, periodo, FuenteEvaluacion.COORDINACION, "5.0");
    escenario.registrarCalificacion(coordinador, periodo, FuenteEvaluacion.AUTOEVALUACION, "4.5");

    mockMvc
        .perform(get("/expedientes/propio").with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resultadoHabilitado").value(true))
        .andExpect(jsonPath("$.promedio.estado").value("COMPLETO"))
        .andExpect(jsonPath("$.promedio.valor").value(4.4));
  }

  @Test
  void laVicerrectoriaNoFirmaActasDeDocentesNiCreaLasDeEllos() throws Exception {
    for (FuenteEvaluacion fuente : FuenteEvaluacion.values()) {
      escenario.registrarCalificacion(docente, periodo, fuente, "4.5");
    }
    citaRepository.saveAndFlush(new Cita(franja(coordinador), docente));

    crearActa(vicerrector, docente).andExpect(status().isNotFound());
    Long acta = idDe(crearActa(coordinador, docente).andExpect(status().isCreated()));
    firmar(acta, vicerrector).andExpect(status().isForbidden());
  }

  private FranjaHoraria franja(Usuario duenio) {
    return franjaRepository.saveAndFlush(
        new FranjaHoraria(
            duenio, periodo, BaseDatosPruebaConfig.HOY.plusDays(1), LocalTime.of(9, 0)));
  }

  private ResultActions crearActa(Usuario evaluador, Usuario evaluado) throws Exception {
    return mockMvc.perform(
        post("/actas")
            .with(como(evaluador))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new NuevaActaRequest(evaluado.getIdUsuario()))));
  }

  private ResultActions firmar(Long acta, Usuario firmante) throws Exception {
    return mockMvc.perform(
        post("/actas/{id}/firmas", acta)
            .with(como(firmante))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new FirmaActaRequest(true))));
  }

  private Long idDe(ResultActions respuesta) throws Exception {
    String cuerpo = respuesta.andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(cuerpo, "$.idActa")).longValue();
  }
}
