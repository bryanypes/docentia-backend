package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Clase;
import com.uniautonoma.docentia.entity.Curso;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.Matricula;
import com.uniautonoma.docentia.entity.OrigenCalificacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.TipoFormulario;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.repository.MatriculaRepository;
import com.uniautonoma.docentia.service.formulario.CatalogoInstrumento;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/** Heteroevaluación del estudiante (HU-01): anonimato, k-anonimato, alcance e integración. */
@ExtendWith(OutputCaptureExtension.class)
class HeteroevaluacionTest extends PruebaIntegracion {

  private static final List<String> HETERO =
      List.of("D02", "D03", "D04", "D05", "D06", "D07", "D08", "D09", "D10", "D11", "D12");
  private static final String LISTA = "/estudiante/evaluaciones";

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CatalogoInstrumento catalogo;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private CalificacionRepository calificacionRepository;
  @Autowired private MatriculaRepository matriculaRepository;
  @Autowired private jakarta.persistence.EntityManager entityManager;

  private Facultad ingenieria;
  private Programa software;
  private PeriodoAcademico periodo;
  private Usuario docente;
  private Usuario coordinador;
  private Usuario coordinadorOtra;
  private Usuario vicerrector;
  private Clase clase;
  private Long asignacion;
  private final List<Usuario> estudiantes = new ArrayList<>();

  @BeforeEach
  void crearEscenario() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    software = escenario.crearPrograma("Software", ingenieria);
    docente = escenario.crearDocente("Docente Uno", software);
    coordinador = escenario.crearCoordinador("Coordinador Uno", ingenieria);
    coordinadorOtra =
        escenario.crearCoordinador("Coordinador Dos", escenario.crearFacultad("Derecho"));
    vicerrector = escenario.crearVicerrector("Vicerrector Uno");
    periodo =
        escenario.crearPeriodoActivoConRecoleccion(
            "2026-2",
            BaseDatosPruebaConfig.HOY.minusDays(1),
            BaseDatosPruebaConfig.HOY.plusDays(9));
    Curso curso = escenario.crearCurso("ISW-101", "Programación web", software);
    clase = escenario.crearClase(periodo, curso, "A", docente);
    for (int i = 1; i <= 7; i++) {
      Usuario estudiante = escenario.crearEstudiante("Estudiante " + i, software);
      escenario.matricular(clase, estudiante);
      estudiantes.add(estudiante);
    }
    asignacion =
        jdbcTemplate.queryForObject(
            "select id from clases_docente where clase_id = ?", Long.class, clase.getId());
  }

  private static Map<String, Object> todos(int valor) {
    Map<String, Object> respuestas = new LinkedHashMap<>();
    HETERO.forEach(codigo -> respuestas.put(codigo, valor));
    return respuestas;
  }

  private ResultActions enviar(
      Usuario estudiante, Map<String, Object> respuestas, String comentario) throws Exception {
    Map<String, Object> cuerpo = new LinkedHashMap<>();
    cuerpo.put("respuestas", respuestas);
    cuerpo.put("comentario", comentario);
    return mockMvc.perform(
        post(LISTA + "/{id}/enviar", asignacion)
            .with(como(estudiante))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(cuerpo)));
  }

  private ResultActions enviar(Usuario estudiante, int valor) throws Exception {
    return enviar(estudiante, todos(valor), null);
  }

  @Test
  void elEnvioRechazadoQuedaAuditadoComoFallo() throws Exception {
    long inexistente = 987655L;

    mockMvc
        .perform(
            post(LISTA + "/{id}/enviar", inexistente)
                .with(como(estudiantes.get(0)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("respuestas", todos(4)))))
        .andExpect(status().isNotFound());

    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from auditoria where accion = 'ENVIO_HETEROEVALUACION'"
                    + " and entidad_id = ? and resultado = 'FALLO'",
                Long.class,
                inexistente))
        .isEqualTo(1);
  }

  private com.uniautonoma.docentia.entity.Calificacion estudiantesDe(Usuario quien) {
    entityManager.flush();
    entityManager.clear();
    return calificacionRepository
        .findByDocenteIdUsuarioAndPeriodoIdPeriodoAndFuente(
            quien.getIdUsuario(), periodo.getIdPeriodo(), FuenteEvaluacion.ESTUDIANTES)
        .orElse(null);
  }

  @Test
  void elFiltroHeteroDevuelveExactamenteOnceDescriptores() {
    assertThat(
            catalogo.de(TipoFormulario.HETERO).stream()
                .flatMap(c -> c.descriptores().stream())
                .map(CatalogoInstrumento.Descriptor::codigo)
                .toList())
        .containsExactlyInAnyOrderElementsOf(HETERO);
  }

  @Test
  void elEstudianteVeSusAsignacionesYElFormularioSinNotas() throws Exception {
    mockMvc
        .perform(get(LISTA).with(como(estudiantes.get(0))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pendientes").value(1))
        .andExpect(jsonPath("$.evaluaciones[0].idAsignacion").value(asignacion))
        .andExpect(jsonPath("$.evaluaciones[0].docente").value("Docente Uno"))
        .andExpect(jsonPath("$.evaluaciones[0].estado").value("PENDIENTE"));
    mockMvc
        .perform(get(LISTA + "/{id}", asignacion).with(como(estudiantes.get(0))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(11))
        .andExpect(jsonPath("$.aviso").value(containsString("anónimas")))
        .andExpect(jsonPath("$.criterios[*].descriptores[*].codigo", hasSize(11)))
        .andExpect(jsonPath("$.criterios[0].descriptores[0].valor").doesNotExist());
  }

  @Test
  void elEnvioValidaItemsValoresYComentario() throws Exception {
    Usuario e = estudiantes.get(0);
    Map<String, Object> incompleto = todos(4);
    incompleto.remove("D12");
    enviar(e, incompleto, null).andExpect(status().isUnprocessableEntity());
    Map<String, Object> fuera = todos(4);
    fuera.put("D02", 6);
    enviar(e, fuera, null).andExpect(status().isUnprocessableEntity());
    Map<String, Object> decimal = todos(4);
    decimal.put("D02", new BigDecimal("4.5"));
    enviar(e, decimal, null).andExpect(status().isUnprocessableEntity());
    Map<String, Object> ajeno = todos(4);
    ajeno.put("D01", 4);
    enviar(e, ajeno, null).andExpect(status().isUnprocessableEntity());
    enviar(e, todos(4), "x".repeat(1001)).andExpect(status().isUnprocessableEntity());

    enviar(e, todos(4), "Explica con ejemplos")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("ENVIADA"))
        .andExpect(content().string(not(containsString("4"))));
  }

  @Test
  void elDobleEnvioResponde409YNoCreaOtraRespuesta() throws Exception {
    enviar(estudiantes.get(0), 4).andExpect(status().isOk());
    enviar(estudiantes.get(0), 5).andExpect(status().isConflict());

    assertThat(contar("respuestas_heteroevaluacion")).isEqualTo(1);
    mockMvc
        .perform(get(LISTA).with(como(estudiantes.get(0))))
        .andExpect(jsonPath("$.evaluaciones[0].estado").value("ENVIADA"));
  }

  @Test
  void fueraDeLaVentanaResponde409() throws Exception {
    jdbcTemplate.update(
        "update etapas_periodo set fecha_apertura = ?, fecha_cierre = ? where id_periodo = ?",
        java.sql.Date.valueOf(LocalDate.of(2026, 9, 1)),
        java.sql.Date.valueOf(LocalDate.of(2026, 9, 2)),
        periodo.getIdPeriodo());
    entityManager.clear();

    enviar(estudiantes.get(0), 4).andExpect(status().isConflict());
    mockMvc
        .perform(get(LISTA + "/{id}", asignacion).with(como(estudiantes.get(0))))
        .andExpect(jsonPath("$.ventanaAbierta").value(false));
  }

  @Test
  void otraAsignacionOtroEstudianteYRetiradoResponden404() throws Exception {
    Usuario ajeno = escenario.crearEstudiante("Estudiante Ajeno", software);
    mockMvc
        .perform(get(LISTA + "/{id}", asignacion).with(como(ajeno)))
        .andExpect(status().isNotFound());
    enviar(ajeno, 4).andExpect(status().isNotFound());
    mockMvc
        .perform(get(LISTA + "/{id}", 987654).with(como(estudiantes.get(0))))
        .andExpect(status().isNotFound());

    Matricula matricula =
        matriculaRepository
            .findByClaseIdAndEstudianteIdUsuarioAndEsActivaTrue(
                clase.getId(), estudiantes.get(1).getIdUsuario())
            .orElseThrow();
    matricula.setEsActiva(false);
    matriculaRepository.saveAndFlush(matricula);
    enviar(estudiantes.get(1), 4).andExpect(status().isNotFound());
    mockMvc
        .perform(get(LISTA).with(como(estudiantes.get(1))))
        .andExpect(jsonPath("$.evaluaciones", hasSize(0)));
  }

  @Test
  void otroPeriodoNoMuestraLasAsignaciones() throws Exception {
    PeriodoAcademico anterior = escenario.crearPeriodoCerrado("2026-1", LocalDate.of(2026, 2, 2));
    mockMvc
        .perform(
            get(LISTA)
                .param("periodo", anterior.getIdPeriodo().toString())
                .with(como(estudiantes.get(0))))
        .andExpect(jsonPath("$.evaluaciones", hasSize(0)));
  }

  @Test
  void otrosRolesNoUsanLasRutasDelEstudianteYElEstudianteNoVeResultados() throws Exception {
    for (Usuario quien : List.of(docente, coordinador, vicerrector)) {
      mockMvc.perform(get(LISTA).with(como(quien))).andExpect(status().isForbidden());
    }
    Usuario e = estudiantes.get(0);
    mockMvc.perform(get("/expedientes/propio").with(como(e))).andExpect(status().isForbidden());
    mockMvc
        .perform(get("/cobertura-heteroevaluacion").with(como(e)))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/kpis/panel").with(como(e))).andExpect(status().isForbidden());
    mockMvc.perform(get("/docentes").with(como(e))).andExpect(status().isForbidden());
  }

  @Test
  void laRespuestaNoTieneNadaQueLaUnaAlEstudiante(CapturedOutput salida) throws Exception {
    enviar(estudiantes.get(0), todos(4), "Comentario de prueba").andExpect(status().isOk());

    List<String> columnas =
        jdbcTemplate.queryForList(
            "select column_name from information_schema.columns where table_name in"
                + " ('respuestas_heteroevaluacion', 'respuestas_descriptor_hetero')",
            String.class);
    assertThat(columnas)
        .noneMatch(
            c ->
                c.contains("estudiante")
                    || c.contains("usuario")
                    || c.contains("fecha")
                    || c.contains("creada")
                    || c.contains("enviada")
                    || c.contains("orden"));
    assertThat(
            jdbcTemplate.queryForObject(
                "select data_type from information_schema.columns"
                    + " where table_name = 'respuestas_heteroevaluacion' and column_name = 'id'",
                String.class))
        .isEqualTo("uuid");
    String idRespuesta =
        jdbcTemplate.queryForObject(
            "select id::text from respuestas_heteroevaluacion", String.class);
    assertThat(salida.getAll())
        .doesNotContain(idRespuesta)
        .doesNotContain("Comentario de prueba")
        .doesNotContain(estudiantes.get(0).getCorreo());
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from auditoria where entidad_id::text = ? or entidad = ?",
                Long.class,
                idRespuesta,
                idRespuesta))
        .isZero();
  }

  @Test
  void conMenosDelMinimoNoReemplazaElCsvYConElMinimoSi() throws Exception {
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.ESTUDIANTES, "3.0");
    for (int i = 0; i < 4; i++) {
      enviar(estudiantes.get(i), 5).andExpect(status().isOk());
    }
    assertThat(estudiantesDe(docente).getOrigen()).isNotEqualTo(OrigenCalificacion.FORMULARIO);
    assertThat(estudiantesDe(docente).getNotas().promedio()).isEqualByComparingTo("3.0");

    enviar(estudiantes.get(4), 5).andExpect(status().isOk());

    assertThat(estudiantesDe(docente).getOrigen()).isEqualTo(OrigenCalificacion.FORMULARIO);
    assertThat(estudiantesDe(docente).getNotaFuente()).isEqualByComparingTo("5");
  }

  @Test
  void sinCsvYBajoElMinimoLaFuenteQuedaFaltante() throws Exception {
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.COORDINACION, "4.0");
    for (int i = 0; i < 4; i++) {
      enviar(estudiantes.get(i), 5).andExpect(status().isOk());
    }
    mockMvc
        .perform(get("/expedientes/{id}", docente.getIdUsuario()).with(como(coordinador)))
        .andExpect(jsonPath("$.promedio.estado").value("PARCIAL"))
        .andExpect(
            jsonPath("$.promedio.fuentesPendientes", org.hamcrest.Matchers.hasItem("ESTUDIANTES")));
  }

  @Test
  void laNotaIntegraLasTresFuentesConLaMediaDeLasMedias() throws Exception {
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.COORDINACION, "4.0");
    escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.AUTOEVALUACION, "4.8");
    for (int i = 0; i < 6; i++) {
      enviar(estudiantes.get(i), i < 3 ? 5 : 4).andExpect(status().isOk());
    }

    assertThat(estudiantesDe(docente).getNotaFuente()).isEqualByComparingTo("4.5");
    mockMvc
        .perform(get("/expedientes/{id}", docente.getIdUsuario()).with(como(coordinador)))
        .andExpect(jsonPath("$.promedio.valor").value(4.41))
        .andExpect(jsonPath("$.promedio.categoria.codigo").value("ALTO"));
  }

  @Test
  void elCsvNoPisaLaFuenteQueYaVieneDelFormulario() throws Exception {
    for (int i = 0; i < 5; i++) {
      enviar(estudiantes.get(i), 5).andExpect(status().isOk());
    }
    byte[] csv =
        ("Correo docente;Dominio disciplinar;Planeación académica;Metodología de enseñanza;"
                + "Evaluación del aprendizaje;Compromiso institucional;Comentario\n"
                + docente.getCorreo()
                + ";2.0;2.0;2.0;2.0;2.0;Bien\n")
            .getBytes(StandardCharsets.UTF_8);
    mockMvc
        .perform(
            multipart("/importaciones/csv")
                .file(new MockMultipartFile("archivo", "e.csv", "text/csv", csv))
                .with(como(coordinador)))
        .andExpect(status().isOk());

    assertThat(estudiantesDe(docente).getOrigen()).isEqualTo(OrigenCalificacion.FORMULARIO);
    assertThat(estudiantesDe(docente).getNotaFuente()).isEqualByComparingTo("5");
  }

  @Test
  void laCoberturaRespetaElAlcanceYNoIdentificaEstudiantes() throws Exception {
    enviar(estudiantes.get(0), 4).andExpect(status().isOk());
    enviar(estudiantes.get(1), 4).andExpect(status().isOk());
    Matricula retirada =
        matriculaRepository
            .findByClaseIdAndEstudianteIdUsuarioAndEsActivaTrue(
                clase.getId(), estudiantes.get(6).getIdUsuario())
            .orElseThrow();
    retirada.setEsActiva(false);
    matriculaRepository.saveAndFlush(retirada);

    mockMvc
        .perform(get("/cobertura-heteroevaluacion").with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total.matriculados").value(6))
        .andExpect(jsonPath("$.total.respondieron").value(2))
        .andExpect(jsonPath("$.total.porcentaje").value(33.3))
        .andExpect(jsonPath("$.docentes.contenido[0].nombre").value("Docente Uno"))
        .andExpect(content().string(not(containsString("Estudiante"))));
    mockMvc
        .perform(get("/cobertura-heteroevaluacion").with(como(coordinadorOtra)))
        .andExpect(jsonPath("$.total.matriculados").value(0));
    mockMvc
        .perform(get("/cobertura-heteroevaluacion").param("size", "500").with(como(vicerrector)))
        .andExpect(jsonPath("$.docentes.tamano").value(50))
        .andExpect(jsonPath("$.total.matriculados").value(6));
    mockMvc
        .perform(get("/cobertura-heteroevaluacion").with(como(docente)))
        .andExpect(status().isForbidden());
  }

  private long contar(String tabla) {
    return jdbcTemplate.queryForObject("select count(*) from " + tabla, Long.class);
  }

  private ResultActions importar(Usuario quien, String csv) throws Exception {
    return mockMvc.perform(
        multipart("/matriculas/csv")
            .file(
                new MockMultipartFile(
                    "archivo", "matricula.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))
            .with(como(quien)));
  }

  private String csvMatricula(String... filas) {
    return "\uFEFFcorreo_estudiante;nombre_estudiante;programa;codigo_curso;nombre_curso;correo_docente;estado\n"
        + String.join("\n", filas)
        + "\n";
  }

  @Test
  void laMatriculaSeImportaTodoONadaEsIdempotenteYRespetaElAlcance() throws Exception {
    String fila =
        " Nuevo.Estudiante@uniautonoma.edu.co ;Nuevo Estudiante;Software;BD-201;Bases de datos;"
            + docente.getCorreo()
            + ";";
    importar(coordinador, csvMatricula(fila))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estudiantesCreados").value(1))
        .andExpect(jsonPath("$.matriculasNuevas").value(1));
    importar(coordinador, csvMatricula(fila))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estudiantesCreados").value(0))
        .andExpect(jsonPath("$.matriculasNuevas").value(0));
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from usuarios where correo = 'nuevo.estudiante@uniautonoma.edu.co'"
                    + " and rol = 'ESTUDIANTE'",
                Long.class))
        .isEqualTo(1);

    long antes = contar("matriculas");
    importar(
            coordinador,
            csvMatricula(
                "otro@uniautonoma.edu.co;Otro;Software;BD-201;Bases de datos;"
                    + docente.getCorreo()
                    + ";",
                "x@gmail.com;X;Software;BD-201;Bases de datos;" + docente.getCorreo() + ";",
                "y@uniautonoma.edu.co;Y;Software;BD-201;Bases;nadie@uniautonoma.edu.co;"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.valido").value(false))
        .andExpect(jsonPath("$.errores[0].fila").value(3))
        .andExpect(jsonPath("$.errores[1].fila").value(4));
    assertThat(contar("matriculas")).isEqualTo(antes);

    importar(coordinadorOtra, csvMatricula(fila))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errores[0].motivo").value("El docente no es de tu facultad"));
    importar(vicerrector, csvMatricula(fila)).andExpect(status().isOk());
    importar(docente, csvMatricula(fila)).andExpect(status().isForbidden());
    importar(estudiantes.get(0), csvMatricula(fila)).andExpect(status().isForbidden());
  }

  @Test
  void unaFilaRetiradaDesactivaLaMatricula() throws Exception {
    Usuario e = estudiantes.get(0);
    String fila = e.getCorreo() + ";E;Software;BD-301;Bases de datos;" + docente.getCorreo() + ";";
    importar(coordinador, csvMatricula(fila + "ACTIVA")).andExpect(status().isOk());
    Long nueva =
        jdbcTemplate.queryForObject(
            "select cd.id from clases_docente cd join clases c on c.id = cd.clase_id"
                + " join cursos cu on cu.id = c.id_curso where cu.codigo = 'BD-301'",
            Long.class);
    mockMvc.perform(get(LISTA + "/{id}", nueva).with(como(e))).andExpect(status().isOk());

    importar(coordinador, csvMatricula(fila + "RETIRADA"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.matriculasRetiradas").value(1));

    mockMvc.perform(get(LISTA + "/{id}", nueva).with(como(e))).andExpect(status().isNotFound());
  }

  @Test
  void laPlantillaSeDescarga() throws Exception {
    mockMvc
        .perform(get("/matriculas/plantilla").with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("correo_estudiante")));
  }
}
