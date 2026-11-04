package com.uniautonoma.docentia.controller;

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
import com.uniautonoma.docentia.dto.SolicitudAnalisisRequest;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.service.analisis.ProcesadorSolicitudesAnalisis;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** Panorama del análisis de comentarios: conteos del alcance, sin textos. */
class PanoramaAnalisisTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;
  @Autowired private ProcesadorSolicitudesAnalisis procesador;
  @Autowired private EntityManager entityManager;

  private PeriodoAcademico periodo;
  private Usuario julian;
  private Usuario diana;
  private Usuario coordinador;

  @BeforeEach
  void crearEscenario() {
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    var software = escenario.crearPrograma("Ingeniería de Software", ingenieria);
    julian = escenario.crearDocente("Julian Hoyos", software);
    diana = escenario.crearDocente("Diana Astaiza", software);
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    periodo =
        escenario.abrirEtapa(
            escenario.crearPeriodoActivo("2026-2"),
            TipoEtapa.REVISION_HALLAZGOS,
            BaseDatosPruebaConfig.HOY,
            BaseDatosPruebaConfig.HOY);
    List<String> comentarios = new ArrayList<>();
    for (int i = 1; i <= 12; i++) {
      comentarios.add(i % 2 == 0 ? "Las notas se demoran en el corte " + i : "Domina el tema " + i);
    }
    escenario.cargarComentarios(julian, periodo, coordinador, comentarios);
  }

  @Test
  void cuentaComentariosAnalisisYHallazgosSinExponerTextos() throws Exception {
    mockMvc
        .perform(
            post("/analisis-ia/solicitudes")
                .with(como(coordinador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        new SolicitudAnalisisRequest(
                            julian.getIdUsuario(), periodo.getIdPeriodo()))))
        .andExpect(status().isAccepted());
    entityManager.flush();
    entityManager.clear();
    while (procesador.procesarSiguiente()) {
      entityManager.flush();
      entityManager.clear();
    }

    mockMvc
        .perform(get("/analisis-ia/panorama").with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodo.nombre").value("2026-2"))
        .andExpect(jsonPath("$.totales.docentes").value(2))
        .andExpect(jsonPath("$.totales.conComentarios").value(1))
        .andExpect(jsonPath("$.totales.comentarios").value(12))
        .andExpect(jsonPath("$.totales.analizados").value(1))
        .andExpect(jsonPath("$.totales.pendientesRevision").value(1))
        .andExpect(jsonPath("$.totales.publicados").value(0))
        .andExpect(jsonPath("$.totales.porDecidir").value(Matchers.greaterThan(0)))
        .andExpect(jsonPath("$.docentes", hasSize(2)))
        .andExpect(
            jsonPath("$.docentes[?(@.docente.idDocente == %d)].estado", julian.getIdUsuario())
                .value(Matchers.contains("COMPLETADO")))
        .andExpect(
            jsonPath("$.docentes[?(@.docente.idDocente == %d)].estado", diana.getIdUsuario())
                .value(Matchers.contains(Matchers.nullValue())))
        .andExpect(content().string(not(Matchers.containsString("Domina el tema"))));
  }

  @Test
  void buscaUnaPalabraEnLosComentariosSinTildesYSoloDevuelveConteos() throws Exception {
    mockMvc
        .perform(
            get("/analisis-ia/panorama/coincidencias")
                .param("buscar", "DEMORÁN")
                .with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].idDocente").value(julian.getIdUsuario()))
        .andExpect(jsonPath("$[0].coincidencias").value(6))
        .andExpect(content().string(not(Matchers.containsString("corte"))));
    mockMvc
        .perform(get("/analisis-ia/panorama/coincidencias").param("buscar", "50%").with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
    mockMvc
        .perform(get("/analisis-ia/panorama/coincidencias").param("buscar", "de").with(como(coordinador)))
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void elDocenteNoConsultaElPanorama() throws Exception {
    mockMvc
        .perform(get("/analisis-ia/panorama").with(como(julian)))
        .andExpect(status().isForbidden());
  }
}
