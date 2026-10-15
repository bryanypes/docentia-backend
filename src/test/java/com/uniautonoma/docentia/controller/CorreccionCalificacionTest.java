package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.CalificacionRequest;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.NotasAspectos;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CorreccionCalificacionRepository;
import com.uniautonoma.docentia.service.CalificacionService;
import com.uniautonoma.docentia.service.CalificacionService.ResultadoRegistro;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

/** D2: corregir una nota recalcula el promedio y deja el rastro en el historial. */
class CorreccionCalificacionTest extends PruebaIntegracion {

  private static final String RUTA_HISTORIAL = "/docentes/{id}/calificaciones/correcciones";

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CalificacionService calificacionService;
  @Autowired private CorreccionCalificacionRepository correccionRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private Facultad ingenieria;
  private PeriodoAcademico periodo;
  private Usuario docente;
  private Usuario coordinador;
  private Usuario vicerrector;

  @BeforeEach
  void crearEscenario() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    docente =
        escenario.crearDocente(
            "Julian Hoyos", escenario.crearPrograma("Ingeniería de Software", ingenieria));
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    vicerrector = escenario.crearVicerrector("Elena Munoz");
    periodo = escenario.crearPeriodoActivo("2026-2");
  }

  @Test
  void corregirLaNotaDelCoordinadorGuardaNotasYPromediosAntesYDespues() throws Exception {
    calificacionService.registrarCalificacionEstudiantes(docente, periodo, notas("4.0"));
    calificar(vicerrector, "vicerrectoria", "5.0", null).andExpect(status().isOk());
    calificar(coordinador, "coordinacion", "3.0", null).andExpect(status().isOk());
    Timestamp calculoAnterior = fechaCalculo();

    calificar(coordinador, "coordinacion", "5.0", "Error de digitación en el acta de visita")
        .andExpect(status().isOk());

    // 4.0*50% + 3.0*30% + 5.0*20% = 3.90  ->  4.0*50% + 5.0*30% + 5.0*20% = 4.50
    mockMvc
        .perform(get(RUTA_HISTORIAL, docente.getIdUsuario()).with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].fuente").value("COORDINACION"))
        .andExpect(jsonPath("$[0].notaAnterior").value(3.0))
        .andExpect(jsonPath("$[0].notaNueva").value(5.0))
        .andExpect(jsonPath("$[0].promedioAnterior.estado").value("COMPLETO"))
        .andExpect(jsonPath("$[0].promedioAnterior.valor").value(3.9))
        .andExpect(jsonPath("$[0].promedioNuevo.valor").value(4.5))
        .andExpect(jsonPath("$[0].nombreUsuario").value("Marta Ruiz"))
        .andExpect(jsonPath("$[0].motivo").value("Error de digitación en el acta de visita"))
        .andExpect(jsonPath("$[0].nombrePeriodo").value("2026-2"))
        .andExpect(jsonPath("$[0].fechaCorreccion").exists());
    assertThat(fechaCalculo()).isAfter(calculoAnterior);
  }

  @Test
  void unaCorreccionConElPromedioIncompletoTambienQuedaRegistrada() throws Exception {
    calificar(coordinador, "coordinacion", "3.0", null);
    calificar(coordinador, "coordinacion", "3.5", null);

    mockMvc
        .perform(get(RUTA_HISTORIAL, docente.getIdUsuario()).with(comoVicerrector()))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].promedioAnterior.estado").value("INCOMPLETO"))
        .andExpect(jsonPath("$[0].promedioAnterior.valor").doesNotExist())
        .andExpect(jsonPath("$[0].motivo").doesNotExist());
  }

  @Test
  void laPrimeraCalificacionYRepetirLasMismasNotasNoSonCorrecciones() throws Exception {
    calificar(coordinador, "coordinacion", "4.0", null);
    // 4.00 guardado en la base y 4.0 en la petición son la misma nota.
    calificar(coordinador, "coordinacion", "4.00", "Solo cambia la observación");

    assertThat(correccionRepository.count()).isZero();
  }

  @Test
  void laCorreccionDeEstudiantesGuardaQuienCargoYElMotivo() {
    calificacionService.registrarCalificacionEstudiantes(docente, periodo, notas("4.0"));

    ResultadoRegistro resultado =
        calificacionService.registrarCalificacionEstudiantes(
            docente, periodo, notas("3.0"), coordinador.getIdUsuario(), "Carga CSV #7");
    ResultadoRegistro repetido =
        calificacionService.registrarCalificacionEstudiantes(
            docente, periodo, notas("3.0"), coordinador.getIdUsuario(), "Carga CSV #8");

    assertThat(resultado).isEqualTo(ResultadoRegistro.CORREGIDA);
    assertThat(repetido).isEqualTo(ResultadoRegistro.SIN_CAMBIOS);
    assertThat(correccionRepository.findAll())
        .singleElement()
        .satisfies(
            correccion -> {
              assertThat(correccion.getFuente()).isEqualTo(FuenteEvaluacion.ESTUDIANTES);
              assertThat(correccion.getMotivo()).isEqualTo("Carga CSV #7");
              assertThat(correccion.getUsuario().getIdUsuario())
                  .isEqualTo(coordinador.getIdUsuario());
            });
  }

  @Test
  void elHistorialSeFiltraPorPeriodo() throws Exception {
    PeriodoAcademico anterior = escenario.crearPeriodoCerrado("2026-1", LocalDate.of(2026, 2, 2));
    calificacionService.registrarCalificacionEstudiantes(docente, anterior, notas("4.0"));
    calificacionService.registrarCalificacionEstudiantes(docente, anterior, notas("4.5"));
    calificar(coordinador, "coordinacion", "3.0", null);
    calificar(coordinador, "coordinacion", "3.5", null);

    mockMvc
        .perform(get(RUTA_HISTORIAL, docente.getIdUsuario()).with(comoVicerrector()))
        .andExpect(jsonPath("$", hasSize(2)));
    mockMvc
        .perform(
            get(RUTA_HISTORIAL, docente.getIdUsuario())
                .param("idPeriodo", anterior.getIdPeriodo().toString())
                .with(comoVicerrector()))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].nombrePeriodo").value("2026-1"))
        .andExpect(jsonPath("$[0].nombreUsuario").doesNotExist());
  }

  @Test
  void elCoordinadorDeOtraFacultadRecibeNoEncontradoYElDocenteNoTieneAcceso() throws Exception {
    calificar(coordinador, "coordinacion", "3.0", null);
    calificar(coordinador, "coordinacion", "3.5", null);
    Usuario coordinadorDerecho =
        escenario.crearCoordinador("Paola Sarria", escenario.crearFacultad("Derecho"));

    mockMvc
        .perform(get(RUTA_HISTORIAL, docente.getIdUsuario()).with(como(coordinadorDerecho)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("Docente no encontrado"));
    mockMvc
        .perform(get(RUTA_HISTORIAL, docente.getIdUsuario()).with(como(docente)))
        .andExpect(status().isForbidden());
  }

  @Test
  void elMotivoTieneLimiteDeLongitud() throws Exception {
    calificar(coordinador, "coordinacion", "3.0", "x".repeat(501))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.motivoCorreccion").exists());
  }

  private ResultActions calificar(Usuario evaluador, String fuente, String nota, String motivo)
      throws Exception {
    BigDecimal valor = new BigDecimal(nota);
    return mockMvc.perform(
        put("/docentes/{id}/calificaciones/" + fuente, docente.getIdUsuario())
            .with(como(evaluador))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                aJson(new CalificacionRequest(valor, valor, valor, valor, valor, null, motivo))));
  }

  private Timestamp fechaCalculo() {
    return jdbcTemplate.queryForObject(
        "select fecha_calculo from promedios_docente where id_docente = ? and id_periodo = ?",
        Timestamp.class,
        docente.getIdUsuario(),
        periodo.getIdPeriodo());
  }

  private NotasAspectos notas(String nota) {
    BigDecimal valor = new BigDecimal(nota);
    return new NotasAspectos(valor, valor, valor, valor, valor);
  }
}
