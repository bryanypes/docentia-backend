package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Clase;
import com.uniautonoma.docentia.entity.Curso;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.OrigenCalificacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.repository.ComentarioEstudianteRepository;
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

/** Heteroevaluación del Anexo 1: un formulario por clase y docente, agregado a ESTUDIANTES. */
class FormulariosHeteroevaluacionTest extends PruebaIntegracion {

  private static final List<String> HETERO =
      List.of("D02", "D03", "D04", "D05", "D06", "D07", "D08", "D09", "D10", "D11", "D12");
  private static final String LISTA = "/formularios/heteroevaluacion";
  private static final String UNO = "/formularios/heteroevaluacion/{idClase}/{idDocente}";

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CalificacionRepository calificacionRepository;
  @Autowired private ComentarioEstudianteRepository comentarioRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private jakarta.persistence.EntityManager entityManager;

  private Usuario karen;
  private Usuario andrea;
  private Usuario zulema;
  private Usuario martin;
  private Usuario docente;
  private Clase claseZulemaA;
  private Clase claseZulemaB;
  private Clase claseMartin;
  private PeriodoAcademico periodo;

  @BeforeEach
  void crearEscenario() {
    Facultad facultad = escenario.crearFacultad("Administración");
    Programa programa = escenario.crearPrograma("Administración de empresas", facultad);
    karen = escenario.crearEstudiante("Karen Mancilla", programa);
    andrea = escenario.crearEstudiante("Andrea Nieto", programa);
    zulema = escenario.crearDocente("Zulema Leon", programa);
    martin = escenario.crearDocente("Martin Valencia", programa);
    docente = escenario.crearDocente("Docente Ajeno", programa);
    periodo =
        escenario.crearPeriodoActivoConRecoleccion(
            "2026-2",
            BaseDatosPruebaConfig.HOY.minusDays(1),
            BaseDatosPruebaConfig.HOY.plusDays(9));
    Curso moviles =
        escenario.crearCurso("12190626", "Desarrollo de aplicaciones móviles", programa);
    Curso creatividad = escenario.crearCurso("19072209", "Creatividad e innovación", programa);
    claseZulemaA = escenario.crearClase(periodo, moviles, "A", zulema);
    claseZulemaB = escenario.crearClase(periodo, moviles, "B", zulema);
    claseMartin = escenario.crearClase(periodo, creatividad, "E", martin);
    escenario.matricular(claseZulemaA, karen);
    escenario.matricular(claseMartin, karen);
    escenario.matricular(claseZulemaB, andrea);
  }

  @Test
  void elEstudianteSoloVeLasClasesEnLasQueEstaMatriculado() throws Exception {
    mockMvc
        .perform(get(LISTA).with(como(karen)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itemsHeteroevaluacion").value(11))
        .andExpect(jsonPath("$.pendientes").value(2))
        .andExpect(jsonPath("$.clases", hasSize(2)))
        .andExpect(jsonPath("$.clases[*].grupo", containsInAnyOrder("A", "E")));
    mockMvc
        .perform(get(LISTA).with(como(andrea)))
        .andExpect(jsonPath("$.clases", hasSize(1)))
        .andExpect(jsonPath("$.clases[0].grupo").value("B"))
        .andExpect(jsonPath("$.clases[0].idDocente").value(zulema.getIdUsuario()));
  }

  @Test
  void elFormularioTieneOnceItemsDelAnexo() throws Exception {
    mockMvc
        .perform(get(UNO, claseZulemaA.getId(), zulema.getIdUsuario()).with(como(karen)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tipo").value("HETERO"))
        .andExpect(jsonPath("$.total").value(11))
        .andExpect(jsonPath("$.respondidos").value(0))
        .andExpect(jsonPath("$.grupo").value("A"))
        .andExpect(jsonPath("$.idClase").value(claseZulemaA.getId()));
  }

  @Test
  void elBorradorSeConservaYElEnvioIncompletoResponde422() throws Exception {
    guardar(karen, claseZulemaA, zulema, Map.of("D02", 4))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("BORRADOR"))
        .andExpect(jsonPath("$.respondidos").value(1));
    enviar(karen, claseZulemaA, zulema).andExpect(status().isUnprocessableEntity());
  }

  @Test
  void laMediaDeLosFormulariosEnviadosEsLaNotaDeEstudiantes() throws Exception {
    guardar(karen, claseMartin, martin, hetero(5)).andExpect(status().isOk());
    enviar(karen, claseMartin, martin).andExpect(status().isOk());
    assertThat(
            calificacionRepository
                .findByDocenteIdUsuarioAndPeriodoIdPeriodoAndFuente(
                    martin.getIdUsuario(), periodo.getIdPeriodo(), FuenteEvaluacion.ESTUDIANTES)
                .orElseThrow()
                .getNotaFuente())
        .isEqualByComparingTo("5.0000");

    guardar(karen, claseZulemaA, zulema, hetero(5)).andExpect(status().isOk());
    enviar(karen, claseZulemaA, zulema).andExpect(status().isOk());
    guardar(andrea, claseZulemaB, zulema, hetero(3)).andExpect(status().isOk());
    enviar(andrea, claseZulemaB, zulema).andExpect(status().isOk());

    var notaZulema =
        calificacionRepository
            .findByDocenteIdUsuarioAndPeriodoIdPeriodoAndFuente(
                zulema.getIdUsuario(), periodo.getIdPeriodo(), FuenteEvaluacion.ESTUDIANTES)
            .orElseThrow();
    assertThat(notaZulema.getNotaFuente()).isEqualByComparingTo("4.0000");
    assertThat(notaZulema.getOrigen()).isEqualTo(OrigenCalificacion.FORMULARIO);
  }

  @Test
  void losComentariosVanAlAnalisisSinImportacionNiIdentidad() throws Exception {
    mockMvc
        .perform(
            put(UNO, claseMartin.getId(), martin.getIdUsuario())
                .with(como(karen))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        Map.of(
                            "respuestas",
                            hetero(5),
                            "comentarios",
                            Map.of("C2", "Explica con claridad")))))
        .andExpect(status().isOk());
    enviar(karen, claseMartin, martin).andExpect(status().isOk());

    assertThat(comentarioRepository.count()).isEqualTo(1);
    assertThat(comentarioRepository.findAll().getFirst().getImportacion()).isNull();
    assertThat(comentarioRepository.findAll().getFirst().getTexto())
        .isEqualTo("Explica con claridad");
  }

  @Test
  void unaClaseAjenaOUnDocenteQueNoDictaRespondenIgualQueSiNoExistieran() throws Exception {
    mockMvc
        .perform(get(UNO, claseZulemaB.getId(), zulema.getIdUsuario()).with(como(karen)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get(UNO, claseZulemaA.getId(), martin.getIdUsuario()).with(como(karen)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get(UNO, 999_999, zulema.getIdUsuario()).with(como(karen)))
        .andExpect(status().isNotFound());
  }

  @Test
  void elDocenteYElCoordinadorNoEntranAlFormularioDelEstudiante() throws Exception {
    mockMvc.perform(get(LISTA).with(como(zulema))).andExpect(status().isForbidden());
    mockMvc.perform(get(LISTA).with(como(docente))).andExpect(status().isForbidden());
  }

  @Test
  void fueraDeLaVentanaYYaEnviadoResponden409() throws Exception {
    guardar(karen, claseMartin, martin, hetero(4)).andExpect(status().isOk());
    enviar(karen, claseMartin, martin).andExpect(status().isOk());
    enviar(karen, claseMartin, martin).andExpect(status().isConflict());
    guardar(karen, claseMartin, martin, hetero(5)).andExpect(status().isConflict());

    jdbcTemplate.update(
        "update etapas_periodo set fecha_apertura = ?, fecha_cierre = ? where id_periodo = ?",
        java.sql.Date.valueOf(LocalDate.of(2026, 9, 1)),
        java.sql.Date.valueOf(LocalDate.of(2026, 9, 2)),
        periodo.getIdPeriodo());
    entityManager.clear();
    guardar(andrea, claseZulemaB, zulema, hetero(3)).andExpect(status().isConflict());
  }

  private static Map<String, Object> hetero(int valor) {
    Map<String, Object> respuestas = new LinkedHashMap<>();
    HETERO.forEach(codigo -> respuestas.put(codigo, valor));
    return respuestas;
  }

  private ResultActions guardar(
      Usuario quien, Clase clase, Usuario profesor, Map<String, Object> respuestas)
      throws Exception {
    return mockMvc.perform(
        put(UNO, clase.getId(), profesor.getIdUsuario())
            .with(como(quien))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(Map.of("respuestas", respuestas))));
  }

  private ResultActions enviar(Usuario quien, Clase clase, Usuario profesor) throws Exception {
    return mockMvc.perform(
        post(UNO + "/enviar", clase.getId(), profesor.getIdUsuario()).with(como(quien)));
  }
}
