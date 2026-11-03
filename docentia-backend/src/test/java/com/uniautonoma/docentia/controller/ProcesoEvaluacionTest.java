package com.uniautonoma.docentia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.AsistenciaCitaRequest;
import com.uniautonoma.docentia.dto.CompromisoActaRequest;
import com.uniautonoma.docentia.dto.ConfirmacionCitaRequest;
import com.uniautonoma.docentia.dto.FirmaActaRequest;
import com.uniautonoma.docentia.dto.NuevaActaRequest;
import com.uniautonoma.docentia.dto.TipoActaRequest;
import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.Clase;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.TipoActa;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CitaRepository;
import com.uniautonoma.docentia.repository.FranjaHorariaRepository;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Estado general del proceso: evaluaciones, cita con confirmación, asistencia que habilita el
 * resultado al docente, tipo de acta y firma en la reunión.
 */
@TestPropertySource(
    properties = {
      "docentia.proceso.exigir-asistencia=true",
      "docentia.actas.exigir-firma-dibujada=true"
    })
class ProcesoEvaluacionTest extends PruebaIntegracion {

  private static final LocalDate HOY = BaseDatosPruebaConfig.HOY;

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CitaRepository citaRepository;
  @Autowired private FranjaHorariaRepository franjaRepository;

  private Facultad ingenieria;
  private Programa software;
  private Usuario coordinador;
  private Usuario docente;
  private PeriodoAcademico periodo;

  @BeforeEach
  void crearEscenario() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    software = escenario.crearPrograma("Software", ingenieria);
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    docente = escenario.crearDocente("Nora Munoz", software);
    periodo =
        escenario.abrirEtapa(
            escenario.crearPeriodoActivo("2026-2"), TipoEtapa.FIRMA_ACTAS, HOY, HOY);
  }

  @Test
  void elProcesoAvanzaDeLasEvaluacionesHastaElActaFirmada() throws Exception {
    proceso().andExpect(jsonPath("$.estado").value("EVALUACIONES_EN_CURSO"));
    procesoPropio()
        .andExpect(jsonPath("$.estado").value("EVALUACIONES_EN_CURSO"))
        .andExpect(jsonPath("$.fuentesPendientes", hasSize(0)))
        .andExpect(jsonPath("$.hitos", hasSize(0)));

    calificar(FuenteEvaluacion.ESTUDIANTES, "4.5");
    calificar(FuenteEvaluacion.COORDINACION, "4.5");
    proceso().andExpect(jsonPath("$.estado").value("PENDIENTE_AUTOEVALUACION"));
    procesoPropio()
        .andExpect(jsonPath("$.estado").value("PENDIENTE_AUTOEVALUACION"))
        .andExpect(jsonPath("$.fuentesPendientes[0]").value("AUTOEVALUACION"));

    calificar(FuenteEvaluacion.AUTOEVALUACION, "4.5");
    proceso().andExpect(jsonPath("$.estado").value("EVALUACION_COMPLETA"));

    Long cita = agendarCita(HOY, LocalTime.of(8, 0));
    proceso()
        .andExpect(jsonPath("$.estado").value("CITA_AGENDADA"))
        .andExpect(jsonPath("$.cita.idCita").value(cita))
        .andExpect(jsonPath("$.resultadoHabilitado").value(false));

    // Antes de la reunión el docente no ve su nota ni el acta, y nadie puede firmarla.
    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(jsonPath("$.promedio.estado").value("COMPLETO"))
        .andExpect(jsonPath("$.promedio.valor").doesNotExist())
        .andExpect(jsonPath("$.promedio.categoria").doesNotExist())
        .andExpect(jsonPath("$.dimensiones[0].resultado").doesNotExist())
        .andExpect(jsonPath("$.resultadoHabilitado").value(false));
    Long acta = crearActa(TipoActa.COMPROMISO);
    mockMvc
        .perform(get("/actas").with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
    mockMvc.perform(get("/actas/{id}", acta).with(como(docente))).andExpect(status().isNotFound());
    firmar(acta, coordinador)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value(containsString("asistencia")));

    registrarAsistencia(cita, true)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.asistencia").value("ASISTIO"));
    proceso()
        .andExpect(jsonPath("$.estado").value("EN_REUNION"))
        .andExpect(jsonPath("$.resultadoHabilitado").value(true))
        .andExpect(jsonPath("$.acta.tipo").value("COMPROMISO"));
    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(jsonPath("$.promedio.valor").value(4.5))
        .andExpect(jsonPath("$.resultadoHabilitado").value(true));
    mockMvc
        .perform(get("/actas").with(como(docente)))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].tipo").value("COMPROMISO"))
        .andExpect(jsonPath("$[0].requiereCompromisos").value(true));

    agregarCompromiso(acta).andExpect(status().isCreated());
    firmarCon(acta, coordinador, null)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.mensaje").value(containsString("Dibuja tu firma")));
    firmarCon(acta, coordinador, png(false))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.mensaje").value(containsString("Dibuja tu firma")));
    firmar(acta, coordinador)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firmas[0].conTrazo").value(true));
    proceso()
        .andExpect(jsonPath("$.estado").value("ACTA_EN_FIRMA"))
        .andExpect(jsonPath("$.acta.firmadaPorCoordinacion").value(true));
    firmar(acta, docente).andExpect(jsonPath("$.estado").value("FIRMADA"));
    byte[] pdf =
        mockMvc
            .perform(get("/actas/{id}/pdf", acta).with(como(docente)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    PdfReader lector = new PdfReader(pdf);
    StringBuilder texto = new StringBuilder();
    for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
      texto.append(new PdfTextExtractor(lector).getTextFromPage(pagina));
    }
    org.assertj.core.api.Assertions.assertThat(texto.toString())
        .contains("Acta de compromiso docente")
        .contains("Firma dibujada en DOCENTIA");

    proceso()
        .andExpect(jsonPath("$.estado").value("FINALIZADO"))
        .andExpect(jsonPath("$.paso").value(8))
        .andExpect(
            jsonPath("$.hitos[*].codigo").value(hasItems("RESERVA", "ASISTENCIA", "ACTA", "FIRMA")))
        .andExpect(jsonPath("$.hitos[*].codigo").value(hasItem("CIERRE")));
  }

  @Test
  void elDocenteConfirmaODeclinaYDeclinarLiberaLaFranja() throws Exception {
    calificarTodo("4.0");
    Long cita = agendarCita(HOY.plusDays(1), LocalTime.of(9, 0));

    confirmar(true)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.confirmacion").value("CONFIRMADA"));
    proceso()
        .andExpect(jsonPath("$.estado").value("ASISTENCIA_CONFIRMADA"))
        .andExpect(jsonPath("$.hitos[*].codigo").value(hasItem("CONFIRMACION")));
    confirmar(true).andExpect(status().isConflict());

    confirmar(false)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.confirmacion").value("DECLINADA"))
        .andExpect(jsonPath("$.estado").value("LIBERADA"));
    mockMvc.perform(get("/citas/mia").with(como(docente))).andExpect(status().isNoContent());
    Long franja = citaRepository.findById(cita).orElseThrow().getFranja().getIdFranja();
    mockMvc
        .perform(get("/franjas/disponibles").with(como(docente)))
        .andExpect(jsonPath("$[*].idFranja").value(hasItem(franja.intValue())));
    proceso()
        .andExpect(jsonPath("$.estado").value("EVALUACION_COMPLETA"))
        .andExpect(jsonPath("$.hitos[*].codigo").value(hasItem("DECLINACION")));
    confirmar(true).andExpect(status().isNotFound());
  }

  @Test
  void laAsistenciaSeRegistraElDiaDeLaCitaYConElResultadoCompleto() throws Exception {
    Long futura = agendarCita(HOY.plusDays(1), LocalTime.of(9, 0));
    registrarAsistencia(futura, true)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value(containsString("día de la cita")));

    Long hoy = agendarCita(HOY, LocalTime.of(8, 0));
    registrarAsistencia(hoy, true)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value(containsString("tres evaluaciones")));

    calificarTodo("3.5");
    confirmar(true)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value(containsString("ya empezó")));
    registrarAsistencia(hoy, false)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.asistencia").value("NO_ASISTIO"))
        .andExpect(jsonPath("$.estado").value("LIBERADA"));
    registrarAsistencia(hoy, true).andExpect(status().isConflict());
    proceso()
        .andExpect(jsonPath("$.estado").value("NO_ASISTIO"))
        .andExpect(jsonPath("$.resultadoHabilitado").value(false))
        .andExpect(jsonPath("$.hitos[*].codigo").value(hasItem("INASISTENCIA")));
  }

  @Test
  void soloLaCoordinacionDelDocenteRegistraLaAsistencia() throws Exception {
    calificarTodo("4.0");
    Long cita = agendarCita(HOY, LocalTime.of(8, 0));
    Usuario otraCoordinacion =
        escenario.crearCoordinador("Paola Sarria", escenario.crearFacultad("Derecho"));

    mockMvc
        .perform(
            post("/citas/{id}/asistencia", cita)
                .with(como(otraCoordinacion))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new AsistenciaCitaRequest(true))))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/citas/{id}/asistencia", cita)
                .with(como(docente))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new AsistenciaCitaRequest(true))))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/citas/mia/confirmacion")
                .with(como(coordinador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new ConfirmacionCitaRequest(true))))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            get("/procesos-evaluacion/{id}", docente.getIdUsuario()).with(como(otraCoordinacion)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/procesos-evaluacion").with(como(docente)))
        .andExpect(status().isForbidden());
  }

  @Test
  void conNotaBajaElActaSiempreEsDeCompromisoYPorEncimaLaCoordinacionElige() throws Exception {
    calificarTodo("2.9");
    mockMvc
        .perform(
            post("/actas")
                .with(como(coordinador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        new NuevaActaRequest(docente.getIdUsuario(), TipoActa.RETROALIMENTACION))))
        .andExpect(status().isConflict());
    Long acta = crearActa(null);
    mockMvc
        .perform(get("/actas/{id}", acta).with(como(coordinador)))
        .andExpect(jsonPath("$.tipo").value("COMPROMISO"));
    cambiarTipo(acta, TipoActa.RETROALIMENTACION).andExpect(status().isConflict());

    Usuario destacado = escenario.crearDocente("Luis Paz", software);
    for (FuenteEvaluacion fuente : FuenteEvaluacion.values()) {
      escenario.registrarCalificacion(destacado, periodo, fuente, "4.7");
    }
    Long otra =
        idActa(
            mockMvc
                .perform(
                    post("/actas")
                        .with(como(coordinador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aJson(new NuevaActaRequest(destacado.getIdUsuario()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("RETROALIMENTACION"))
                .andExpect(jsonPath("$.requiereCompromisos").value(false)));
    cambiarTipo(otra, TipoActa.COMPROMISO)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tipo").value("COMPROMISO"))
        .andExpect(jsonPath("$.requiereCompromisos").value(true));
  }

  @Test
  void elListadoDaElEstadoDeCadaDocenteYLaCoberturaDeEstudiantes() throws Exception {
    Clase clase =
        escenario.crearClase(
            periodo, escenario.crearCurso("IS101", "Programación", software), "A", docente);
    escenario.matricular(clase, escenario.crearEstudiante("Karen Ruiz", software));
    escenario.matricular(clase, escenario.crearEstudiante("Andrea Paz", software));
    calificar(FuenteEvaluacion.COORDINACION, "4.0");

    mockMvc
        .perform(get("/procesos-evaluacion").with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].docente.idDocente").value(docente.getIdUsuario()))
        .andExpect(jsonPath("$[0].estado").value("EVALUACIONES_EN_CURSO"))
        .andExpect(jsonPath("$[0].fuentesPendientes").value(hasItem("ESTUDIANTES")))
        .andExpect(jsonPath("$[0].fuentesPendientes").value(not(hasItem("COORDINACION"))))
        .andExpect(jsonPath("$[0].coberturaEstudiantes.esperadas").value(2))
        .andExpect(jsonPath("$[0].coberturaEstudiantes.respondidas").value(0))
        .andExpect(jsonPath("$[0].coberturaEstudiantes.minimoRespuestas").value(5))
        .andExpect(jsonPath("$[0].coberturaEstudiantes.origenNota").doesNotExist());
    // Con nota de estudiantes que no viene de formularios, el proceso dice de dónde salió.
    calificar(FuenteEvaluacion.ESTUDIANTES, "4.0");
    mockMvc
        .perform(get("/procesos-evaluacion").with(como(coordinador)))
        .andExpect(jsonPath("$[0].coberturaEstudiantes.origenNota").value("MANUAL"));
    mockMvc
        .perform(get("/procesos-evaluacion").with(comoVicerrector()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[*].docente.idDocente").value(hasItem(docente.getIdUsuario().intValue())));
    procesoPropio().andExpect(jsonPath("$.coberturaEstudiantes").doesNotExist());
  }

  private void calificar(FuenteEvaluacion fuente, String nota) {
    escenario.registrarCalificacion(docente, periodo, fuente, nota);
  }

  private void calificarTodo(String nota) {
    for (FuenteEvaluacion fuente : FuenteEvaluacion.values()) {
      calificar(fuente, nota);
    }
  }

  private Long agendarCita(LocalDate fecha, LocalTime hora) {
    FranjaHoraria franja =
        franjaRepository.saveAndFlush(new FranjaHoraria(coordinador, periodo, fecha, hora));
    citaRepository
        .buscarActiva(docente.getIdUsuario(), periodo.getIdPeriodo())
        .ifPresent(
            anterior -> {
              anterior.liberar(com.uniautonoma.docentia.entity.MotivoLiberacion.REEMPLAZADA);
              citaRepository.saveAndFlush(anterior);
            });
    return citaRepository.saveAndFlush(new Cita(franja, docente)).getIdCita();
  }

  private ResultActions proceso() throws Exception {
    return mockMvc
        .perform(get("/procesos-evaluacion/{id}", docente.getIdUsuario()).with(como(coordinador)))
        .andExpect(status().isOk());
  }

  private ResultActions procesoPropio() throws Exception {
    return mockMvc
        .perform(get("/procesos-evaluacion/propio").with(como(docente)))
        .andExpect(status().isOk());
  }

  private ResultActions confirmar(boolean asiste) throws Exception {
    return mockMvc.perform(
        post("/citas/mia/confirmacion")
            .with(como(docente))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new ConfirmacionCitaRequest(asiste))));
  }

  private ResultActions registrarAsistencia(Long cita, boolean asistio) throws Exception {
    return mockMvc.perform(
        post("/citas/{id}/asistencia", cita)
            .with(como(coordinador))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new AsistenciaCitaRequest(asistio))));
  }

  private Long crearActa(TipoActa tipo) throws Exception {
    return idActa(
        mockMvc
            .perform(
                post("/actas")
                    .with(como(coordinador))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(aJson(new NuevaActaRequest(docente.getIdUsuario(), tipo))))
            .andExpect(status().isCreated()));
  }

  private ResultActions cambiarTipo(Long acta, TipoActa tipo) throws Exception {
    return mockMvc.perform(
        patch("/actas/{id}/tipo", acta)
            .with(como(coordinador))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new TipoActaRequest(tipo))));
  }

  private ResultActions agregarCompromiso(Long acta) throws Exception {
    return mockMvc.perform(
        post("/actas/{id}/compromisos", acta)
            .with(como(coordinador))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                aJson(
                    new CompromisoActaRequest(
                        "Actualizar el plan de asignatura", HOY.plusMonths(2)))));
  }

  private ResultActions firmar(Long acta, Usuario firmante) throws Exception {
    return firmarCon(acta, firmante, png(true));
  }

  private ResultActions firmarCon(Long acta, Usuario firmante, String trazo) throws Exception {
    return mockMvc.perform(
        post("/actas/{id}/firmas", acta)
            .with(como(firmante))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new FirmaActaRequest(true, trazo))));
  }

  /** Un trazo como el del lienzo de la página, o un lienzo en blanco. */
  private static String png(boolean conTrazo) throws Exception {
    BufferedImage lienzo = new BufferedImage(400, 150, BufferedImage.TYPE_INT_ARGB);
    if (conTrazo) {
      Graphics2D lapiz = lienzo.createGraphics();
      lapiz.setColor(Color.BLACK);
      lapiz.setStroke(new BasicStroke(4));
      lapiz.drawLine(20, 100, 180, 40);
      lapiz.drawLine(180, 40, 360, 110);
      lapiz.dispose();
    }
    ByteArrayOutputStream salida = new ByteArrayOutputStream();
    ImageIO.write(lienzo, "png", salida);
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(salida.toByteArray());
  }

  private Long idActa(ResultActions respuesta) throws Exception {
    String cuerpo = respuesta.andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(cuerpo, "$.idActa")).longValue();
  }
}
