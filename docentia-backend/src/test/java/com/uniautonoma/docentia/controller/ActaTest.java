package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.CompromisoActaRequest;
import com.uniautonoma.docentia.dto.CorreccionActaRequest;
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
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

/** G1, G3 y G4: firma con cita, PDF único, inmutabilidad y corrección por versiones. */
class ActaTest extends PruebaIntegracion {

  private static final java.time.LocalDate PLAZO = BaseDatosPruebaConfig.HOY.plusMonths(2);

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CitaRepository citaRepository;
  @Autowired private FranjaHorariaRepository franjaRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManager entityManager;

  private Facultad ingenieria;
  private Usuario coordinador;
  private Usuario docente;
  private Usuario vicerrector;
  private PeriodoAcademico periodo;

  @BeforeEach
  void crearEscenario() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
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
  void conNotaBajaExigeCompromisoYElPdfTraeElPlanDeMejoramiento() throws Exception {
    calificarTodo("2.9");
    agendarCita();
    Long acta = crearActa();

    firmar(acta, coordinador, true)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("3.8")));

    agregarCompromiso(acta, "Actualizar el plan de asignatura").andExpect(status().isCreated());
    firmar(acta, coordinador, true)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("BORRADOR"))
        .andExpect(jsonPath("$.firmas", hasSize(1)));
    firmar(acta, docente, true)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("FIRMADA"))
        .andExpect(jsonPath("$.firmas", hasSize(2)))
        .andExpect(jsonPath("$.categoria.codigo").value("BAJO"))
        .andExpect(jsonPath("$.codigoVerificacion").isNotEmpty())
        .andExpect(jsonPath("$.hashPdf").isNotEmpty());

    byte[] pdf =
        mockMvc
            .perform(get("/actas/{id}/pdf", acta).with(como(docente)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    PdfReader lector = new PdfReader(pdf);
    String texto = new PdfTextExtractor(lector).getTextFromPage(1);
    assertThat(texto)
        .contains("Plan de Mejoramiento Profesoral")
        .contains("Actualizar el plan de asignatura")
        .contains("Marta Ruiz")
        .contains("Nora Munoz")
        .contains("SHA-256");
    assertThat(texto).doesNotContain("Nota de estudiantes");

    byte[] otraVez =
        mockMvc
            .perform(get("/actas/{id}/pdf", acta).with(como(coordinador)))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(otraVez).isEqualTo(pdf);
  }

  @Test
  void conNotaEnElUmbralBastaLaConfirmacionDeLectura() throws Exception {
    calificarTodo("3.8");
    agendarCita();
    Long acta = crearActa();

    firmar(acta, coordinador, true).andExpect(status().isOk());
    firmar(acta, docente, true).andExpect(jsonPath("$.estado").value("FIRMADA"));
  }

  @Test
  void laFirmaExigeCitaYConfirmacionYNoSeRepite() throws Exception {
    calificarTodo("4.5");
    Long acta = crearActa();

    firmar(acta, coordinador, true)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("cita")));
    agendarCita();
    firmar(acta, coordinador, false).andExpect(status().isUnprocessableEntity());
    firmar(acta, coordinador, true).andExpect(status().isOk());
    firmar(acta, coordinador, true).andExpect(status().isConflict());
  }

  @Test
  void sinLasTresFuentesNoSeFirma() throws Exception {
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.COORDINACION, "4.0");
    agendarCita();
    Long acta = crearActa();

    firmar(acta, coordinador, true).andExpect(status().isConflict());
  }

  @Test
  void fueraDeLaVentanaDeFirmaResponde409ConLaEtapa() throws Exception {
    calificarTodo("4.5");
    agendarCita();
    Long acta = crearActa();
    escenario.abrirEtapa(
        entityManager.find(PeriodoAcademico.class, periodo.getIdPeriodo()),
        TipoEtapa.FIRMA_ACTAS,
        BaseDatosPruebaConfig.HOY.plusDays(2),
        BaseDatosPruebaConfig.HOY.plusDays(3));

    firmar(acta, coordinador, true)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.etapa").value("FIRMA_ACTAS"));
  }

  @Test
  void unActaFirmadaEsInmutableTambienEnLaBase() throws Exception {
    Long acta = actaFirmada();

    agregarCompromiso(acta, "Otro compromiso").andExpect(status().isConflict());
    entityManager.flush();
    assertThatThrownBy(
            () -> jdbcTemplate.update("update actas set promedio = 5 where id_acta = ?", acta))
        .hasMessageContaining("inmutable");
  }

  @Test
  void corregirCreaUnaVersionNuevaYAnulaLaAnterior() throws Exception {
    Long acta = actaFirmada();

    Long nueva =
        idDe(
            corregir(acta, vicerrector, "Se recalificó la coordinación")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.idActaAnterior").value(acta))
                .andExpect(jsonPath("$.compromisos", hasSize(1))));

    mockMvc
        .perform(get("/actas").with(como(docente)))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].idActa").value(nueva))
        .andExpect(jsonPath("$[1].estado").value("ANULADA"));
    mockMvc.perform(get("/actas/{id}/pdf", acta).with(como(docente))).andExpect(status().isOk());
  }

  @Test
  void soloElRolConfiguradoCorrigeYConMotivo() throws Exception {
    Long acta = actaFirmada();

    corregir(acta, coordinador, "Quiero cambiarla").andExpect(status().isForbidden());
    corregir(acta, docente, "Quiero cambiarla").andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/actas/{id}/correcciones", acta)
                .with(como(vicerrector))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new CorreccionActaRequest(" "))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.motivo").exists());
  }

  @Test
  void respetaElAlcanceYElDocenteNoVeNotasPorFuente() throws Exception {
    calificarTodo("4.5");
    Long acta = crearActa();
    Usuario otroCoordinador =
        escenario.crearCoordinador("Paola Sarria", escenario.crearFacultad("Derecho"));
    Usuario otroDocente =
        escenario.crearDocente("Luis Paz", escenario.crearPrograma("Electrónica", ingenieria));

    mockMvc
        .perform(get("/actas/{id}", acta).with(como(otroCoordinador)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/actas/{id}", acta).with(como(otroDocente)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/actas/{id}", acta).with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notaEstudiantes").doesNotExist())
        .andExpect(jsonPath("$.notaCoordinacion").doesNotExist())
        .andExpect(jsonPath("$.notaAutoevaluacion").doesNotExist());
    mockMvc
        .perform(
            post("/actas")
                .with(como(docente))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new NuevaActaRequest(docente.getIdUsuario()))))
        .andExpect(status().isForbidden());
  }

  @Test
  void noHayDosActasVigentesYLosCompromisosSeQuitanEnBorrador() throws Exception {
    calificarTodo("2.9");
    Long acta = crearActa();
    mockMvc
        .perform(
            post("/actas")
                .with(como(coordinador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new NuevaActaRequest(docente.getIdUsuario()))))
        .andExpect(status().isConflict());

    String cuerpo =
        agregarCompromiso(acta, "Compromiso").andReturn().getResponse().getContentAsString();
    Number idCompromiso = JsonPath.read(cuerpo, "$.compromisos[0].idCompromiso");
    mockMvc
        .perform(
            delete("/actas/{a}/compromisos/{c}", acta, idCompromiso.longValue())
                .with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.compromisos", hasSize(0)));
  }

  @Test
  void laVerificacionPorCodigoEsPublicaYNoMuestraNotas() throws Exception {
    Long acta = actaFirmada();
    String cuerpo =
        mockMvc
            .perform(get("/actas/{id}", acta).with(como(coordinador)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String codigo = JsonPath.read(cuerpo, "$.codigoVerificacion");

    mockMvc
        .perform(get("/verificacion-actas/{c}", codigo))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombreDocente").value("Nora Munoz"))
        .andExpect(jsonPath("$.estado").value("FIRMADA"))
        .andExpect(jsonPath("$.promedio").doesNotExist());
    mockMvc.perform(get("/verificacion-actas/NOEXISTE")).andExpect(status().isNotFound());
  }

  private Long actaFirmada() throws Exception {
    calificarTodo("2.9");
    agendarCita();
    Long acta = crearActa();
    agregarCompromiso(acta, "Actualizar el plan de asignatura");
    firmar(acta, coordinador, true).andExpect(status().isOk());
    firmar(acta, docente, true).andExpect(jsonPath("$.estado").value("FIRMADA"));
    return acta;
  }

  private void calificarTodo(String nota) {
    for (FuenteEvaluacion fuente : FuenteEvaluacion.values()) {
      escenario.registrarCalificacion(docente, periodo, fuente, nota);
    }
  }

  private void agendarCita() {
    FranjaHoraria franja =
        franjaRepository.saveAndFlush(
            new FranjaHoraria(
                coordinador, periodo, BaseDatosPruebaConfig.HOY.plusDays(1), LocalTime.of(9, 0)));
    citaRepository.saveAndFlush(new Cita(franja, docente));
  }

  private Long crearActa() throws Exception {
    return idDe(
        mockMvc
            .perform(
                post("/actas")
                    .with(como(coordinador))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(aJson(new NuevaActaRequest(docente.getIdUsuario()))))
            .andExpect(status().isCreated()));
  }

  private Long idDe(ResultActions respuesta) throws Exception {
    String cuerpo = respuesta.andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(cuerpo, "$.idActa")).longValue();
  }

  private ResultActions firmar(Long acta, Usuario firmante, boolean confirmacion) throws Exception {
    return mockMvc.perform(
        post("/actas/{id}/firmas", acta)
            .with(como(firmante))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new FirmaActaRequest(confirmacion))));
  }

  private ResultActions agregarCompromiso(Long acta, String descripcion) throws Exception {
    return mockMvc.perform(
        post("/actas/{id}/compromisos", acta)
            .with(como(coordinador))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new CompromisoActaRequest(descripcion, PLAZO))));
  }

  private ResultActions corregir(Long acta, Usuario usuario, String motivo) throws Exception {
    return mockMvc.perform(
        post("/actas/{id}/correcciones", acta)
            .with(como(usuario))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new CorreccionActaRequest(motivo))));
  }
}
