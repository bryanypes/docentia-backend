package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.repository.ComentarioEstudianteRepository;
import com.uniautonoma.docentia.repository.CorreccionCalificacionRepository;
import com.uniautonoma.docentia.repository.ImportacionRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** C4: carga del CSV de evaluación de estudiantes, todo o nada, idempotente y auditada. */
class ImportacionCsvTest extends PruebaIntegracion {

  private static final String ENCABEZADO =
      "Correo docente;Dominio disciplinar;Planeación académica;Metodología de enseñanza;"
          + "Evaluación del aprendizaje;Compromiso institucional;Comentario\n";

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CalificacionRepository calificacionRepository;
  @Autowired private ComentarioEstudianteRepository comentarioRepository;
  @Autowired private ImportacionRepository importacionRepository;
  @Autowired private CorreccionCalificacionRepository correccionRepository;

  private PeriodoAcademico periodo;
  private Usuario julian;
  private Usuario diana;
  private Usuario coordinador;

  @BeforeEach
  void crearEscenario() {
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    Programa software = escenario.crearPrograma("Ingeniería de Software", ingenieria);
    julian = escenario.crearDocente("Julian Hoyos", software);
    diana = escenario.crearDocente("Diana Astaiza", software);
    escenario.crearDocente(
        "Luisa Mosquera", escenario.crearPrograma("Derecho", escenario.crearFacultad("Derecho")));
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    periodo = escenario.crearPeriodoActivo("2026-2");
  }

  @Test
  void validarDevuelveColumnasMapeoYVistaPreviaSinGuardarNada() throws Exception {
    enviar("/importaciones/csv/validar", csvValido(), como(coordinador))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valido").value(true))
        .andExpect(jsonPath("$.delimitador").value(";"))
        .andExpect(jsonPath("$.codificacion").value("UTF-8"))
        .andExpect(jsonPath("$.columnas", hasSize(7)))
        .andExpect(jsonPath("$.mapeo['Correo docente']").value("CORREO_DOCENTE"))
        .andExpect(jsonPath("$.mapeo['Planeación académica']").value("PLANEACION_ACADEMICA"))
        .andExpect(jsonPath("$.filasProcesadas").value(3))
        .andExpect(jsonPath("$.docentes", hasSize(2)))
        .andExpect(jsonPath("$.docentes[0].nombreCompleto").value("Julian Hoyos"))
        .andExpect(jsonPath("$.docentes[0].filas").value(2))
        .andExpect(jsonPath("$.docentes[0].dominioDisciplinar").value(4.25))
        .andExpect(jsonPath("$.docentes[0].comentarios").value(2));

    assertThat(calificacionRepository.count()).isZero();
    assertThat(importacionRepository.count()).isZero();
  }

  @Test
  void importarGuardaNotasYComentariosYRecalculaElPromedio() throws Exception {
    escenario.registrarCalificacion(julian, periodo, FuenteEvaluacion.COORDINACION, "4.0");
    escenario.registrarCalificacion(julian, periodo, FuenteEvaluacion.VICERRECTORIA, "4.0");

    enviar("/importaciones/csv", csvValido(), como(coordinador))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.importacion.resultado").value("EXITOSA"))
        .andExpect(jsonPath("$.importacion.nombreArchivo").value("evaluacion.csv"))
        .andExpect(jsonPath("$.importacion.hashSha256").isString())
        .andExpect(jsonPath("$.importacion.notasNuevas").value(2))
        .andExpect(jsonPath("$.importacion.comentariosNuevos").value(3))
        .andExpect(jsonPath("$.importacion.docentesAfectados").value(2))
        .andExpect(jsonPath("$.reporte.valido").value(true));

    Calificacion notaJulian =
        calificacionRepository
            .findByDocenteIdUsuarioAndPeriodoIdPeriodoAndFuente(
                julian.getIdUsuario(), periodo.getIdPeriodo(), FuenteEvaluacion.ESTUDIANTES)
            .orElseThrow();
    assertThat(notaJulian.getNotas().dominioDisciplinar()).isEqualByComparingTo("4.25");
    assertThat(notaJulian.getImportacion()).isNotNull();
    // Nota de estudiantes = (4.25 + 4 + 4 + 4 + 4) / 5 = 4.05
    // Promedio = 4.05*50% + 4.0*30% + 4.0*20% = 4.025, redondeado a 4.03
    mockMvc
        .perform(get("/expedientes/propio").with(como(julian)))
        .andExpect(jsonPath("$.promedio.estado").value("COMPLETO"))
        .andExpect(jsonPath("$.promedio.valor").value(4.03));
  }

  @Test
  void conUnSoloErrorNoSeImportaNadaYLaCargaQuedaRechazada() throws Exception {
    String csv =
        ENCABEZADO
            + "julian.hoyos@uniautonoma.edu.co;4;4;4;4;4;Bien\n"
            + "diana.astaiza@uniautonoma.edu.co;7;4;4;4;4;\n";

    enviar("/importaciones/csv", archivo(csv), como(coordinador))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.importacion.resultado").value("RECHAZADA"))
        .andExpect(jsonPath("$.importacion.filasConError").value(1))
        .andExpect(jsonPath("$.reporte.valido").value(false))
        .andExpect(jsonPath("$.reporte.errores[0].fila").value(3))
        .andExpect(jsonPath("$.reporte.errores[0].columna").value("Dominio disciplinar"))
        .andExpect(jsonPath("$.reporte.errores[0].valor").value("7"))
        .andExpect(jsonPath("$.reporte.errores[0].motivo").value("La nota debe estar entre 0 y 5"));

    assertThat(calificacionRepository.count()).isZero();
    assertThat(comentarioRepository.count()).isZero();
    assertThat(importacionRepository.count()).isEqualTo(1);
  }

  @Test
  void repetirElMismoArchivoNoDuplicaNotasNiComentarios() throws Exception {
    enviar("/importaciones/csv", csvValido(), como(coordinador)).andExpect(status().isOk());

    enviar("/importaciones/csv", csvValido(), como(coordinador))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.importacion.notasNuevas").value(0))
        .andExpect(jsonPath("$.importacion.notasSinCambios").value(2))
        .andExpect(jsonPath("$.importacion.comentariosNuevos").value(0))
        .andExpect(jsonPath("$.importacion.comentariosRepetidos").value(3))
        .andExpect(jsonPath("$.importacion.docentesAfectados").value(0))
        .andExpect(jsonPath("$.reporte.advertencias[0]", containsString("ya se importó")));

    assertThat(calificacionRepository.count()).isEqualTo(2);
    assertThat(comentarioRepository.count()).isEqualTo(3);
    assertThat(correccionRepository.count()).isZero();
    assertThat(importacionRepository.count()).isEqualTo(2);
  }

  @Test
  void unArchivoConOtrasNotasCorrigeYDejaElRastroEnElHistorial() throws Exception {
    enviar("/importaciones/csv", csvValido(), como(coordinador)).andExpect(status().isOk());
    String corregido = ENCABEZADO + "julian.hoyos@uniautonoma.edu.co;3;3;3;3;3;\n";

    enviar("/importaciones/csv", archivo(corregido), como(coordinador))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.importacion.notasCorregidas").value(1));

    assertThat(correccionRepository.findAll())
        .singleElement()
        .satisfies(
            correccion -> {
              assertThat(correccion.getFuente()).isEqualTo(FuenteEvaluacion.ESTUDIANTES);
              assertThat(correccion.getMotivo()).startsWith("Carga de CSV #");
            });
  }

  @Test
  void dosEstudiantesConElMismoComentarioSeConservanAmbos() throws Exception {
    String csv =
        ENCABEZADO
            + "julian.hoyos@uniautonoma.edu.co;4;4;4;4;4;Excelente\n"
            + "julian.hoyos@uniautonoma.edu.co;4;4;4;4;4;Excelente\n";

    enviar("/importaciones/csv", archivo(csv), como(coordinador)).andExpect(status().isOk());
    enviar("/importaciones/csv", archivo(csv), como(coordinador)).andExpect(status().isOk());

    assertThat(comentarioRepository.count()).isEqualTo(2);
  }

  @Test
  void unDocenteDeOtraFacultadSeReportaComoNoEncontrado() throws Exception {
    String csv = ENCABEZADO + "luisa.mosquera@uniautonoma.edu.co;4;4;4;4;4;\n";

    enviar("/importaciones/csv/validar", archivo(csv), como(coordinador))
        .andExpect(jsonPath("$.valido").value(false))
        .andExpect(jsonPath("$.errores[0].motivo").value("Docente no encontrado"))
        .andExpect(jsonPath("$.errores[0].valor").value("luisa.mosquera@uniautonoma.edu.co"));
  }

  @Test
  void aceptaElCsvDeExcelEnEspanolConPuntoYComaLatin1YComaDecimal() throws Exception {
    byte[] latin1 =
        (ENCABEZADO + "julian.hoyos@uniautonoma.edu.co;4,5;4,5;4;4;4;Buena metodología\n")
            .getBytes(StandardCharsets.ISO_8859_1);

    enviar(
            "/importaciones/csv",
            new MockMultipartFile("archivo", "excel.csv", "text/csv", latin1),
            como(coordinador))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.importacion.codificacion").value("ISO-8859-1"))
        .andExpect(jsonPath("$.reporte.docentes[0].dominioDisciplinar").value(4.5));
    assertThat(comentarioRepository.findAll())
        .singleElement()
        .satisfies(comentario -> assertThat(comentario.getTexto()).isEqualTo("Buena metodología"));
  }

  @Test
  void sinLaColumnaDeUnCampoObligatorioSeRechaza() throws Exception {
    String csv = "Correo docente;Dominio disciplinar\njulian.hoyos@uniautonoma.edu.co;4\n";

    enviar("/importaciones/csv", archivo(csv), como(coordinador))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.reporte.camposObligatoriosSinMapear", hasItem("COMPROMISO_INSTITUCIONAL")))
        .andExpect(jsonPath("$.reporte.errores[0].fila").doesNotExist());
  }

  @Test
  void unArchivoIlegibleSeRechazaConUnMensajeClaro() throws Exception {
    enviar("/importaciones/csv", archivo(""), como(coordinador))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.reporte.errores[0].motivo").value("El archivo está vacío"));
    enviar("/importaciones/csv", archivo(ENCABEZADO), como(coordinador))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.reporte.errores[0].motivo", containsString("sin filas de datos")));
  }

  @Test
  void fueraDeLaRecoleccionNoSeImportaPeroSiSeValida() throws Exception {
    periodo.programarEtapa(
        com.uniautonoma.docentia.entity.TipoEtapa.RECOLECCION_CALIFICACIONES,
        BaseDatosPruebaConfig.HOY.plusDays(1),
        BaseDatosPruebaConfig.HOY.plusDays(5));

    enviar("/importaciones/csv", csvValido(), como(coordinador))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.etapa").value("RECOLECCION_CALIFICACIONES"));
    enviar("/importaciones/csv/validar", csvValido(), como(coordinador))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.advertencias[0]", containsString("no se podrá importar hoy")));
    assertThat(importacionRepository.count()).isZero();
  }

  @Test
  void soloElCoordinadorCargaYCadaUnoVeSuHistorial() throws Exception {
    enviar("/importaciones/csv", csvValido(), como(julian)).andExpect(status().isForbidden());
    enviar("/importaciones/csv", csvValido(), comoVicerrector()).andExpect(status().isForbidden());
    mockMvc
        .perform(multipart("/importaciones/csv").file(csvValido()))
        .andExpect(status().isUnauthorized());

    enviar("/importaciones/csv", csvValido(), como(coordinador)).andExpect(status().isOk());
    Usuario otroCoordinador =
        escenario.crearCoordinador("Pedro Paz", escenario.crearFacultad("Ciencias"));

    mockMvc
        .perform(get("/importaciones").with(como(coordinador)))
        .andExpect(jsonPath("$[*].nombreUsuario", contains("Marta Ruiz")));
    mockMvc
        .perform(get("/importaciones").with(como(otroCoordinador)))
        .andExpect(jsonPath("$", hasSize(0)));
    mockMvc
        .perform(get("/importaciones").with(comoVicerrector()))
        .andExpect(jsonPath("$", hasSize(1)));
    mockMvc.perform(get("/importaciones").with(como(julian))).andExpect(status().isForbidden());
  }

  @Test
  void unArchivoDemasiadoGrandeOSinArchivoRespondenSinTraza() throws Exception {
    byte[] grande = new byte[2 * 1024 * 1024 + 1];

    enviar(
            "/importaciones/csv",
            new MockMultipartFile("archivo", "grande.csv", "text/csv", grande),
            como(coordinador))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.mensaje", containsString("pesa más de")));
    mockMvc
        .perform(multipart("/importaciones/csv").with(como(coordinador)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Falta la parte \"archivo\" del formulario"))
        .andExpect(jsonPath("$.trace").doesNotExist());
  }

  private MockMultipartFile csvValido() {
    return archivo(
        ENCABEZADO
            + "julian.hoyos@uniautonoma.edu.co;4.5;4;4;4;4;Explica muy bien\n"
            + "julian.hoyos@uniautonoma.edu.co;4.0;4;4;4;4;\"Puntual; responde dudas\"\n"
            + "diana.astaiza@uniautonoma.edu.co;5;5;4.5;5;5;Excelente\n");
  }

  private MockMultipartFile archivo(String contenido) {
    return new MockMultipartFile(
        "archivo", "evaluacion.csv", "text/csv", contenido.getBytes(StandardCharsets.UTF_8));
  }

  private ResultActions enviar(String ruta, MockMultipartFile archivo, RequestPostProcessor quien)
      throws Exception {
    return mockMvc.perform(multipart(ruta).file(archivo).with(quien));
  }
}
