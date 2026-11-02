package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.OrigenCalificacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.TipoFormulario;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.service.analisis.ProveedorComentarios;
import com.uniautonoma.docentia.service.formulario.CatalogoInstrumento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

/** Autoevaluación y coevaluación del Anexo 1 de la Resolución 074. */
class FormulariosTest extends PruebaIntegracion {

  private static final List<String> COEVAL = List.of("D01", "D06", "D11", "D12", "D13", "D14");
  private static final String AUTO = "/formularios/autoevaluacion";
  private static final String CO = "/formularios/coevaluacion/{id}";

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CatalogoInstrumento catalogo;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private jakarta.persistence.EntityManager entityManager;
  @Autowired private CalificacionRepository calificacionRepository;
  @Autowired private ProveedorComentarios proveedorComentarios;

  private Facultad ingenieria;
  private Programa software;
  private Usuario docente;
  private Usuario coordinador;
  private Usuario coordinadorOtraFacultad;
  private Usuario vicerrector;
  private PeriodoAcademico periodo;

  @BeforeEach
  void crearEscenario() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    software = escenario.crearPrograma("Software", ingenieria);
    docente = escenario.crearDocente("Docente Uno", software);
    coordinador = escenario.crearCoordinador("Coordinador Uno", ingenieria);
    coordinadorOtraFacultad =
        escenario.crearCoordinador("Coordinador Dos", escenario.crearFacultad("Derecho"));
    vicerrector = escenario.crearVicerrector("Vicerrector Uno");
    periodo =
        escenario.crearPeriodoActivoConRecoleccion(
            "2026-2",
            BaseDatosPruebaConfig.HOY.minusDays(1),
            BaseDatosPruebaConfig.HOY.plusDays(9));
  }

  private static Map<String, Object> todos(int valor) {
    Map<String, Object> respuestas = new LinkedHashMap<>();
    for (int i = 1; i <= 14; i++) {
      respuestas.put(String.format("D%02d", i), valor);
    }
    return respuestas;
  }

  private static Map<String, Object> coevaluacion(int valor) {
    Map<String, Object> respuestas = new LinkedHashMap<>();
    COEVAL.forEach(codigo -> respuestas.put(codigo, valor));
    return respuestas;
  }

  private ResultActions guardarAuto(Map<String, Object> respuestas) throws Exception {
    return mockMvc.perform(
        put(AUTO)
            .with(como(docente))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(Map.of("respuestas", respuestas))));
  }

  private ResultActions enviarAuto() throws Exception {
    return mockMvc.perform(post(AUTO + "/enviar").with(como(docente)));
  }

  private ResultActions guardarCo(Usuario quien, Map<String, Object> respuestas) throws Exception {
    return mockMvc.perform(
        put(CO, docente.getIdUsuario())
            .with(como(quien))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(Map.of("respuestas", respuestas))));
  }

  private ResultActions enviarCo(Usuario quien) throws Exception {
    return mockMvc.perform(post(CO + "/enviar", docente.getIdUsuario()).with(como(quien)));
  }

  private void cerrarRecoleccion() {
    jdbcTemplate.update(
        "update etapas_periodo set fecha_apertura = ?, fecha_cierre = ? where id_periodo = ?",
        java.sql.Date.valueOf(LocalDate.of(2026, 9, 1)),
        java.sql.Date.valueOf(LocalDate.of(2026, 9, 2)),
        periodo.getIdPeriodo());
    entityManager.clear();
  }

  private int contarActor(String tipo) {
    return jdbcTemplate.queryForObject(
        "select count(*) from descriptor_actor where tipo = ?", Integer.class, tipo);
  }

  @Test
  void elCatalogoEsExactamenteElDelAnexo() {
    assertThat(contarActor("AUTO")).isEqualTo(14);
    assertThat(contarActor("HETERO")).isEqualTo(11);
    assertThat(contarActor("COEVAL")).isEqualTo(6);
    assertThat(
            jdbcTemplate.queryForObject("select count(*) from criterios_evaluacion", Integer.class))
        .isEqualTo(6);
    assertThat(
            catalogo.de(TipoFormulario.COEVAL).stream()
                .flatMap(criterio -> criterio.descriptores().stream())
                .map(CatalogoInstrumento.Descriptor::codigo)
                .sorted()
                .toList())
        .containsExactlyElementsOf(COEVAL);
    assertThat(
            jdbcTemplate.queryForObject(
                "select c.codigo from descriptores_evaluacion d join criterios_evaluacion c"
                    + " on c.id = d.criterio_id where d.codigo = 'D08'",
                String.class))
        .isEqualTo("C2");
  }

  @Test
  void elDocenteLeeLaEstructuraDeCatorceItems() throws Exception {
    mockMvc
        .perform(get(AUTO).with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(14))
        .andExpect(jsonPath("$.respondidos").value(0))
        .andExpect(jsonPath("$.instrumentoVersion").value("RES074-2026-ANEXO1"))
        .andExpect(jsonPath("$.ventanaAbierta").value(true))
        .andExpect(jsonPath("$.criterios", hasSize(6)))
        .andExpect(jsonPath("$.escala", hasSize(5)));
  }

  @Test
  void elBorradorPuedeSerParcialYSeConserva() throws Exception {
    guardarAuto(Map.of("D01", 4, "D14", 2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("BORRADOR"))
        .andExpect(jsonPath("$.respondidos").value(2));
    guardarAuto(Map.of("D01", 5)).andExpect(jsonPath("$.respondidos").value(1));
  }

  @Test
  void rechazaValoresFueraDeEscalaDecimalesYCodigosAjenos() throws Exception {
    guardarAuto(Map.of("D01", 6)).andExpect(status().isUnprocessableEntity());
    guardarAuto(Map.of("D01", 0)).andExpect(status().isUnprocessableEntity());
    guardarAuto(Map.of("D01", new BigDecimal("4.5"))).andExpect(status().isUnprocessableEntity());
    guardarAuto(Map.of("D99", 4)).andExpect(status().isUnprocessableEntity());
    guardarCo(coordinador, Map.of("D02", 4)).andExpect(status().isUnprocessableEntity());
  }

  @Test
  void unComentarioDemasiadoLargoSeRechaza() throws Exception {
    mockMvc
        .perform(
            put(AUTO)
                .with(como(docente))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("comentarios", Map.of("C1", "x".repeat(1001))))))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void elEnvioIncompletoResponde422() throws Exception {
    enviarAuto().andExpect(status().isUnprocessableEntity());
    guardarAuto(Map.of("D01", 4)).andExpect(status().isOk());
    enviarAuto().andExpect(status().isUnprocessableEntity());
  }

  @Test
  void fueraDeLaVentanaResponde409() throws Exception {
    cerrarRecoleccion();

    mockMvc
        .perform(get(AUTO).with(como(docente)))
        .andExpect(jsonPath("$.ventanaAbierta").value(false));
    guardarAuto(todos(4)).andExpect(status().isConflict());
    enviarAuto().andExpect(status().isConflict());
    guardarCo(coordinador, coevaluacion(4)).andExpect(status().isConflict());
  }

  @Test
  void unPeriodoCerradoNoSeEdita() throws Exception {
    PeriodoAcademico cerrado = escenario.crearPeriodoCerrado("2026-1", LocalDate.of(2026, 2, 2));
    mockMvc
        .perform(
            put(AUTO)
                .param("periodo", cerrado.getIdPeriodo().toString())
                .with(como(docente))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("respuestas", todos(4)))))
        .andExpect(status().isConflict());
  }

  @Test
  void trasEnviarQuedaInmutable() throws Exception {
    guardarAuto(todos(4)).andExpect(status().isOk());
    enviarAuto()
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("ENVIADA"))
        .andExpect(jsonPath("$.enviadaEn").exists());

    enviarAuto().andExpect(status().isConflict());
    guardarAuto(todos(5)).andExpect(status().isConflict());
  }

  @Test
  void laNotaIntegraLasTresFuentes() throws Exception {
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.ESTUDIANTES, "4.5");
    Map<String, Object> autoevaluacion = todos(5);
    autoevaluacion.put("D01", 4);
    autoevaluacion.put("D02", 4);
    autoevaluacion.put("D03", 4);
    guardarAuto(autoevaluacion).andExpect(status().isOk());
    enviarAuto().andExpect(status().isOk());
    guardarCo(coordinador, coevaluacion(4)).andExpect(status().isOk());
    enviarCo(coordinador).andExpect(status().isOk());

    mockMvc
        .perform(get("/expedientes/{id}", docente.getIdUsuario()).with(como(coordinador)))
        .andExpect(jsonPath("$.promedio.estado").value("COMPLETO"))
        .andExpect(jsonPath("$.promedio.valor").value(4.41))
        .andExpect(jsonPath("$.promedio.categoria.codigo").value("ALTO"));
    var guardada =
        calificacionRepository
            .findByDocenteIdUsuarioAndPeriodoIdPeriodoAndFuente(
                docente.getIdUsuario(), periodo.getIdPeriodo(), FuenteEvaluacion.AUTOEVALUACION)
            .orElseThrow();
    assertThat(guardada.getOrigen()).isEqualTo(OrigenCalificacion.FORMULARIO);
    assertThat(guardada.getNotaFuente()).isEqualByComparingTo("4.7857");
  }

  @Test
  void elFormularioPrevaleceSobreLoCargadoAntesYNoSePuedePisar() throws Exception {
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.ESTUDIANTES, "4.0");
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.COORDINACION, "2.0");
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.AUTOEVALUACION, "2.0");

    guardarCo(coordinador, coevaluacion(5)).andExpect(status().isOk());
    enviarCo(coordinador).andExpect(status().isOk());

    mockMvc
        .perform(get("/expedientes/{id}", docente.getIdUsuario()).with(como(coordinador)))
        .andExpect(jsonPath("$.promedio.valor").value(3.9));
    String calificacion =
        "{\"dominioDisciplinar\":3,\"planeacionAcademica\":3,\"metodologiaEnsenanza\":3,"
            + "\"evaluacionAprendizaje\":3,\"compromisoInstitucional\":3,"
            + "\"motivoCorreccion\":\"Prueba\"}";
    mockMvc
        .perform(
            put("/docentes/{id}/calificaciones/coordinacion", docente.getIdUsuario())
                .with(como(coordinador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(calificacion))
        .andExpect(status().isConflict());
  }

  @Test
  void laCargaDeEstudiantesNoTocaLoQueVinoDeFormulario() throws Exception {
    guardarCo(coordinador, coevaluacion(5)).andExpect(status().isOk());
    enviarCo(coordinador).andExpect(status().isOk());
    byte[] csv =
        ("Correo docente;Dominio disciplinar;Planeación académica;Metodología de enseñanza;"
                + "Evaluación del aprendizaje;Compromiso institucional;Comentario\n"
                + docente.getCorreo()
                + ";4.0;4.0;4.0;4.0;4.0;Bien\n")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    mockMvc
        .perform(
            multipart("/importaciones/csv")
                .file(
                    new org.springframework.mock.web.MockMultipartFile(
                        "archivo", "e.csv", "text/csv", csv))
                .with(como(coordinador)))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.mensaje")
                .value("La evaluación de estudiantes se responde en el formulario"));

    var coevaluacion =
        calificacionRepository
            .findByDocenteIdUsuarioAndPeriodoIdPeriodoAndFuente(
                docente.getIdUsuario(), periodo.getIdPeriodo(), FuenteEvaluacion.COORDINACION)
            .orElseThrow();
    assertThat(coevaluacion.getOrigen()).isEqualTo(OrigenCalificacion.FORMULARIO);
    assertThat(coevaluacion.getNotaFuente()).isEqualByComparingTo("5");
  }

  @Test
  void elAlcanceDeLaCoevaluacionEsLaFacultadYElCoordinadorQueLaEmpezo() throws Exception {
    Long id = docente.getIdUsuario();
    mockMvc
        .perform(get(CO, id).with(como(coordinadorOtraFacultad)))
        .andExpect(status().isNotFound());
    guardarCo(coordinadorOtraFacultad, coevaluacion(1)).andExpect(status().isNotFound());
    enviarCo(coordinadorOtraFacultad).andExpect(status().isNotFound());

    guardarCo(coordinador, coevaluacion(4)).andExpect(status().isOk());

    Usuario colega = escenario.crearCoordinador("Coordinador Tres", ingenieria);
    mockMvc.perform(get(CO, id).with(como(colega))).andExpect(status().isNotFound());
    guardarCo(colega, coevaluacion(1)).andExpect(status().isNotFound());
  }

  @Test
  void elDocenteNuncaObtienePuntajesDeCoevaluacion() throws Exception {
    Long id = docente.getIdUsuario();
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.ESTUDIANTES, "3.0");
    guardarCo(coordinador, coevaluacion(5)).andExpect(status().isOk());
    enviarCo(coordinador).andExpect(status().isOk());

    mockMvc.perform(get(CO, id).with(como(docente))).andExpect(status().isForbidden());
    mockMvc
        .perform(get("/formularios/coevaluacion/pendientes").with(como(docente)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/docentes/{id}/calificaciones/coordinacion", id).with(como(docente)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/expedientes/{id}", id).with(como(docente)))
        .andExpect(status().isForbidden());
    mockMvc.perform(get(AUTO).with(como(docente))).andExpect(jsonPath("$.respondidos").value(0));
    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notasPorFuente").doesNotExist())
        .andExpect(jsonPath("$.promedio.valor").doesNotExist())
        .andExpect(content().string(not(containsString("COORDINACION\":5"))));
  }

  @Test
  void otroDocenteNoLeeLaAutoevaluacionAjena() throws Exception {
    Usuario otro = escenario.crearDocente("Docente Dos", software);
    guardarAuto(todos(5)).andExpect(status().isOk());

    mockMvc
        .perform(get(AUTO).with(como(otro)))
        .andExpect(jsonPath("$.idDocente").value(otro.getIdUsuario()))
        .andExpect(jsonPath("$.respondidos").value(0));
  }

  @Test
  void elCoordinadorVeAvanceYElVicerrectorSoloConteos() throws Exception {
    guardarCo(coordinador, Map.of("D01", 3)).andExpect(status().isOk());

    mockMvc
        .perform(get("/formularios/coevaluacion/pendientes").with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pendientes").value(1))
        .andExpect(jsonPath("$.docentes[0].estado").value("BORRADOR"))
        .andExpect(jsonPath("$.docentes[0].respondidos").value(1))
        .andExpect(jsonPath("$.docentes[0].valor").doesNotExist());
    mockMvc
        .perform(get("/formularios/coevaluacion/pendientes").with(como(vicerrector)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.docentes", hasSize(0)))
        .andExpect(jsonPath("$.programas[0].programa").value("Software"))
        .andExpect(jsonPath("$.programas[0].borrador").value(1))
        .andExpect(content().string(not(containsString("Docente Uno"))));
    mockMvc
        .perform(get("/formularios/coevaluacion/pendientes").with(como(coordinadorOtraFacultad)))
        .andExpect(jsonPath("$.docentes", hasSize(0)));
  }

  @Test
  void losComentariosPorCriterioNoLleganAlAnalisis() throws Exception {
    mockMvc
        .perform(
            put(AUTO)
                .with(como(docente))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("comentarios", Map.of("C1", "Texto solo del formulario")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.criterios[0].comentario").value("Texto solo del formulario"));

    assertThat(proveedorComentarios.comentarios(docente.getIdUsuario(), periodo.getIdPeriodo()))
        .isEmpty();
    assertThat(
            proveedorComentarios.contarEncuestasCerradas(
                docente.getIdUsuario(), periodo.getIdPeriodo()))
        .isZero();
  }

  @Test
  void elEnvioQuedaAuditadoSinPuntajes() throws Exception {
    guardarAuto(todos(4)).andExpect(status().isOk());
    enviarAuto().andExpect(status().isOk());

    mockMvc
        .perform(
            get("/auditoria")
                .param("accion", "ENVIO_FORMULARIO")
                .param("usuario", docente.getCorreo())
                .with(comoVicerrector()))
        .andExpect(jsonPath("$.contenido[0].entidad").value("FORMULARIO_AUTO"))
        .andExpect(jsonPath("$.contenido[0].entidadId").value(docente.getIdUsuario()))
        .andExpect(jsonPath("$.contenido[0].resultado").value("EXITO"))
        .andExpect(jsonPath("$.contenido[0].valor").doesNotExist());
  }
}
