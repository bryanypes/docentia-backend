package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.SolicitudAnalisisRequest;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.analisis.AnalisisIaService;
import com.uniautonoma.docentia.service.analisis.ProcesadorSolicitudesAnalisis;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** E2: los comentarios ofensivos o sobre la persona no llegan al modelo ni se citan. */
class FiltroComentariosAnalisisTest extends PruebaIntegracion {

  private static final String INSULTO = "Ese profesor es un idiota.";
  private static final String APARIENCIA = "Es un profesor muy feo y se viste mal.";

  @Autowired private EscenarioPrueba escenario;
  @Autowired private AnalisisIaService analisisService;
  @Autowired private ProcesadorSolicitudesAnalisis procesador;
  @Autowired private EntityManager entityManager;

  private Usuario coordinador;
  private Usuario julian;
  private List<Long> idsFiltrados;
  private Long idSolicitud;

  @BeforeEach
  void solicitarYProcesar() {
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    julian =
        escenario.crearDocente("Julian Hoyos", escenario.crearPrograma("Software", ingenieria));
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    PeriodoAcademico periodo =
        escenario.abrirEtapa(
            escenario.crearPeriodoActivo("2026-2"),
            TipoEtapa.REVISION_HALLAZGOS,
            BaseDatosPruebaConfig.HOY,
            BaseDatosPruebaConfig.HOY);
    List<String> comentarios = new ArrayList<>();
    comentarios.add(INSULTO);
    for (int i = 1; i <= 10; i++) {
      comentarios.add(
          i % 2 == 0 ? "La metodología es pésima, no explica " + i : "Domina el tema " + i);
    }
    comentarios.add(APARIENCIA);
    List<Long> ids = escenario.cargarComentarios(julian, periodo, coordinador, comentarios);
    idsFiltrados = List.of(ids.get(0), ids.get(ids.size() - 1));
    idSolicitud =
        analisisService
            .solicitar(
                new SolicitudAnalisisRequest(julian.getIdUsuario(), periodo.getIdPeriodo()),
                UsuarioSesion.desde(coordinador))
            .idSolicitud();
    entityManager.flush();
    entityManager.clear();
    procesador.procesarSiguiente();
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void losFiltradosNoSeEnvianNiSeCitanYLaCriticaDuraSiPasa() throws Exception {
    String cuerpo =
        mockMvc
            .perform(get("/analisis-ia/solicitudes/{id}", idSolicitud).with(como(coordinador)))
            .andExpect(jsonPath("$.estado").value("COMPLETADO"))
            .andExpect(jsonPath("$.encuestasCerradas").value(12))
            .andExpect(jsonPath("$.comentariosEnviados").value(10))
            .andExpect(jsonPath("$.comentariosFiltrados").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();

    List<Integer> citados = JsonPath.read(cuerpo, "$.borrador.hallazgos[*].citas[*].idComentario");
    assertThat(citados).isNotEmpty();
    assertThat(citados.stream().map(Integer::longValue)).doesNotContainAnyElementsOf(idsFiltrados);
    List<String> fragmentos = JsonPath.read(cuerpo, "$.borrador.hallazgos[*].citas[*].fragmento");
    assertThat(fragmentos)
        .noneMatch(fragmento -> fragmento.contains("idiota") || fragmento.contains("feo"));
    // "La metodología es pésima" es dura pero sobre la práctica: llega al modelo y se puede citar.
    assertThat(fragmentos).anyMatch(fragmento -> fragmento.startsWith("La metodología es pésima"));
  }

  @Test
  void quedaElRegistroDeCadaComentarioFiltradoConSuMotivo() throws Exception {
    mockMvc
        .perform(
            get("/analisis-ia/solicitudes/{id}/comentarios-filtrados", idSolicitud)
                .with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[*].categoria", containsInAnyOrder("INSULTO", "APARIENCIA")))
        .andExpect(jsonPath("$[*].texto", containsInAnyOrder(INSULTO, APARIENCIA)))
        .andExpect(jsonPath("$[0].regla").isString())
        .andExpect(jsonPath("$[0].fechaFiltro").exists());
  }

  @Test
  void elRegistroDeFiltradosRespetaRolYAlcance() throws Exception {
    Usuario otro = escenario.crearCoordinador("Paola Sarria", escenario.crearFacultad("Derecho"));

    mockMvc
        .perform(
            get("/analisis-ia/solicitudes/{id}/comentarios-filtrados", idSolicitud)
                .with(como(otro)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get("/analisis-ia/solicitudes/{id}/comentarios-filtrados", idSolicitud)
                .with(como(julian)))
        .andExpect(status().isForbidden());
  }
}
