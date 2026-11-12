package com.uniautonoma.docentia.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.DescarteHallazgoRequest;
import com.uniautonoma.docentia.dto.EdicionHallazgoRequest;
import com.uniautonoma.docentia.dto.SolicitudAnalisisRequest;
import com.uniautonoma.docentia.entity.EstadoHallazgo;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Hallazgo;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.SolicitudAnalisisRepository;
import com.uniautonoma.docentia.service.analisis.ProcesadorSolicitudesAnalisis;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** E4: la coordinación incluye, descarta o edita cada hallazgo y publica el resultado. */
class RevisionHallazgosTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;
  @Autowired private ProcesadorSolicitudesAnalisis procesador;
  @Autowired private SolicitudAnalisisRepository solicitudRepository;
  @Autowired private EntityManager entityManager;

  private Usuario julian;
  private Usuario coordinador;
  private SolicitudAnalisis solicitud;
  private List<Hallazgo> hallazgos;

  @BeforeEach
  void crearEscenario() throws Exception {
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
    for (int i = 1; i <= 12; i++) {
      comentarios.add(i % 2 == 0 ? "Las notas se demoran en el corte " + i : "Domina el tema " + i);
    }
    escenario.cargarComentarios(julian, periodo, coordinador, comentarios);
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
    solicitud = solicitudRepository.findAll().get(0);
    hallazgos = solicitud.getHallazgos();
  }

  @Test
  void cadaCambioDejaUnaVersionConElModeloYLaInstruccion() throws Exception {
    Long id = hallazgos.get(0).getIdHallazgo();

    accion("/analisis-ia/hallazgos/{id}/validacion", id).andExpect(status().isOk());
    mockMvc
        .perform(
            put("/analisis-ia/hallazgos/{id}", id)
                .with(como(coordinador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new EdicionHallazgoRequest("Redacción revisada"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.texto").value("Redacción revisada"));
    descartar(id, "No aporta").andExpect(status().isOk());

    mockMvc
        .perform(get("/analisis-ia/hallazgos/{id}/versiones", id).with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[*].accion", contains("GENERADO", "INCLUIDO", "EDITADO", "DESCARTADO")))
        .andExpect(jsonPath("$[0].modelo").value("simulado-determinista"))
        .andExpect(jsonPath("$[0].versionInstruccion").value("analisis-v3"))
        .andExpect(jsonPath("$[0].nombreUsuario").doesNotExist())
        .andExpect(jsonPath("$[3].nombreUsuario").value("Marta Ruiz"))
        .andExpect(jsonPath("$[3].motivo").value("No aporta"));
  }

  @Test
  void noSePublicaConHallazgosSinDecidir() throws Exception {
    publicar().andExpect(status().isConflict());
    descartarTodos();
    publicar().andExpect(status().isConflict());
  }

  @Test
  void elDocenteSoloVeLoIncluidoDespuesDePublicar() throws Exception {
    mockMvc
        .perform(get("/analisis-ia/resultados/propio").with(como(julian)))
        .andExpect(jsonPath("$.hallazgos", hasSize(0)));

    descartarTodos();
    Hallazgo incluido =
        hallazgos.stream().filter(Hallazgo::tieneCitaVerificada).findFirst().orElseThrow();
    accion("/analisis-ia/hallazgos/{id}/validacion", incluido.getIdHallazgo())
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/analisis-ia/resultados/propio").with(como(julian)))
        .andExpect(jsonPath("$.hallazgos", hasSize(0)));

    publicar().andExpect(status().isOk());

    mockMvc
        .perform(get("/analisis-ia/resultados/propio").with(como(julian)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodo.nombre").value("2026-2"))
        .andExpect(jsonPath("$.hallazgos", hasSize(1)))
        .andExpect(jsonPath("$.hallazgos[0].texto").value(incluido.getTexto()))
        .andExpect(jsonPath("$.hallazgos[0].citas").doesNotExist());
  }

  @Test
  void loPublicadoNoCambia() throws Exception {
    descartarTodos();
    Long id = hallazgos.get(0).getIdHallazgo();
    accion("/analisis-ia/hallazgos/{id}/validacion", id).andExpect(status().isOk());
    publicar().andExpect(status().isOk());

    descartar(id, "Tarde").andExpect(status().isConflict());
  }

  @Test
  void respetaRolYAlcance() throws Exception {
    Long id = hallazgos.get(0).getIdHallazgo();
    Usuario otro = escenario.crearCoordinador("Paola Sarria", escenario.crearFacultad("Derecho"));

    mockMvc
        .perform(
            post("/analisis-ia/hallazgos/{id}/descarte", id)
                .with(como(otro))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new DescarteHallazgoRequest(null))))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/analisis-ia/solicitudes/{id}/publicacion", solicitud.getIdSolicitud())
                .with(como(julian)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/analisis-ia/resultados/propio").with(como(coordinador)))
        .andExpect(status().isForbidden());
  }

  @Test
  void elDescarteNoVuelveAPendienteAlRecalcularCitas() {
    Hallazgo hallazgo = hallazgos.get(0);
    hallazgo.descartar();
    hallazgo.actualizarEstadoSegunCitas();
    org.assertj.core.api.Assertions.assertThat(hallazgo.getEstado())
        .isEqualTo(EstadoHallazgo.DESCARTADO);
  }

  private void descartarTodos() throws Exception {
    for (Hallazgo hallazgo : hallazgos) {
      descartar(hallazgo.getIdHallazgo(), null).andExpect(status().isOk());
    }
  }

  private ResultActions descartar(Long id, String motivo) throws Exception {
    return mockMvc.perform(
        post("/analisis-ia/hallazgos/{id}/descarte", id)
            .with(como(coordinador))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new DescarteHallazgoRequest(motivo))));
  }

  private ResultActions accion(String ruta, Long id) throws Exception {
    return mockMvc.perform(post(ruta, id).with(como(coordinador)));
  }

  private ResultActions publicar() throws Exception {
    return mockMvc.perform(
        post("/analisis-ia/solicitudes/{id}/publicacion", solicitud.getIdSolicitud())
            .with(como(coordinador)));
  }
}
