package com.uniautonoma.docentia.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.uniautonoma.docentia.service.analisis.ModeloIA;
import com.uniautonoma.docentia.service.analisis.ModeloIA.RespuestaModelo;
import com.uniautonoma.docentia.service.analisis.ProcesadorSolicitudesAnalisis;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * E3: el modelo propone cuatro hallazgos; el servidor solo da por buenas las citas que aparecen en
 * un comentario no filtrado del docente y periodo.
 */
class CitasVerificablesTest extends PruebaIntegracion {

  @MockitoBean private ModeloIA modelo;

  @Autowired private EscenarioPrueba escenario;
  @Autowired private AnalisisIaService analisisService;
  @Autowired private ProcesadorSolicitudesAnalisis procesador;
  @Autowired private EntityManager entityManager;

  private Usuario coordinador;
  private Usuario julian;
  private PeriodoAcademico periodo;
  private Long idSolicitud;
  private String borrador;

  @BeforeEach
  void generarBorrador() throws Exception {
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    julian =
        escenario.crearDocente("Julian Hoyos", escenario.crearPrograma("Software", ingenieria));
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    periodo =
        escenario.abrirEtapa(
            escenario.crearPeriodoActivo("2026-2"),
            TipoEtapa.REVISION_HALLAZGOS,
            BaseDatosPruebaConfig.HOY,
            BaseDatosPruebaConfig.HOY);
    List<String> textos = new ArrayList<>();
    textos.add("Las notas de los parciales se demoran más de dos semanas.");
    textos.add("Ese profesor es un idiota.");
    for (int i = 1; i <= 8; i++) {
      textos.add("Domina el tema número " + i);
    }
    List<Long> ids = escenario.cargarComentarios(julian, periodo, coordinador, textos);
    Long notas = ids.get(0);
    Long filtrado = ids.get(1);
    when(modelo.analizar(any()))
        .thenReturn(
            new RespuestaModelo(
                """
                {"hallazgos":[
                  {"tipo":"oportunidad_de_mejora","texto":"Publicar notas a tiempo",
                   "citas":[{"comentarioId":%d,"fragmento":"LAS NOTAS de los parciales se demoran"}]},
                  {"tipo":"oportunidad_de_mejora","texto":"Mejorar el trato",
                   "citas":[{"comentarioId":%d,"fragmento":"los estudiantes se sienten ignorados"}]},
                  {"tipo":"oportunidad_de_mejora","texto":"Cuidar el lenguaje",
                   "citas":[{"comentarioId":%d,"fragmento":"Ese profesor es un idiota"}]},
                  {"tipo":"fortaleza","texto":"Es muy querido","citas":[]}
                ]}
                """
                    .formatted(notas, notas, filtrado),
                "modelo-de-prueba"));
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
    borrador =
        mockMvc
            .perform(get("/analisis-ia/solicitudes/{id}", idSolicitud).with(como(coordinador)))
            .andExpect(jsonPath("$.estado").value("COMPLETADO"))
            .andReturn()
            .getResponse()
            .getContentAsString();
  }

  @Test
  void soloLaCitaQueApareceEnUnComentarioNoFiltradoQuedaVerificada() throws Exception {
    mockMvc
        .perform(get("/analisis-ia/solicitudes/{id}", idSolicitud).with(como(coordinador)))
        .andExpect(jsonPath("$.borrador.hallazgos[0].estado").value("PENDIENTE_VALIDACION"))
        .andExpect(jsonPath("$.borrador.hallazgos[0].citas[0].verificada").value(true))
        .andExpect(
            jsonPath("$.borrador.hallazgos[0].citas[0].fragmento")
                .value("LAS NOTAS de los parciales se demoran"))
        // Cita inventada sobre un comentario real.
        .andExpect(jsonPath("$.borrador.hallazgos[1].estado").value("SIN_CITA_VERIFICADA"))
        .andExpect(jsonPath("$.borrador.hallazgos[1].citas[0].verificada").value(false))
        // Cita de un comentario que el filtro excluyó.
        .andExpect(jsonPath("$.borrador.hallazgos[2].estado").value("SIN_CITA_VERIFICADA"))
        .andExpect(jsonPath("$.borrador.hallazgos[2].citas[0].verificada").value(false))
        // Hallazgo sin citas.
        .andExpect(jsonPath("$.borrador.hallazgos[3].estado").value("SIN_CITA_VERIFICADA"));
  }

  @Test
  void unHallazgoConCitaVerificadaSeValidaYUnoSinElla422() throws Exception {
    mockMvc
        .perform(
            post("/analisis-ia/hallazgos/{id}/validacion", hallazgo(0)).with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("VALIDADO"));

    for (int indice = 1; indice <= 3; indice++) {
      mockMvc
          .perform(
              post("/analisis-ia/hallazgos/{id}/validacion", hallazgo(indice))
                  .with(como(coordinador)))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("cita")));
    }
  }

  @Test
  void validarRespetaRolAlcanceYLaVentanaDeRevision() throws Exception {
    Usuario otro = escenario.crearCoordinador("Paola Sarria", escenario.crearFacultad("Derecho"));

    mockMvc
        .perform(post("/analisis-ia/hallazgos/{id}/validacion", hallazgo(0)).with(como(otro)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(post("/analisis-ia/hallazgos/{id}/validacion", hallazgo(0)).with(como(julian)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/analisis-ia/hallazgos/{id}/validacion", hallazgo(0)).with(comoVicerrector()))
        .andExpect(status().isForbidden());

    escenario.abrirEtapa(
        entityManager.find(PeriodoAcademico.class, periodo.getIdPeriodo()),
        TipoEtapa.REVISION_HALLAZGOS,
        BaseDatosPruebaConfig.HOY.plusDays(2),
        BaseDatosPruebaConfig.HOY.plusDays(4));
    mockMvc
        .perform(
            post("/analisis-ia/hallazgos/{id}/validacion", hallazgo(0)).with(como(coordinador)))
        .andExpect(status().isConflict());
  }

  @Test
  void elDocenteNoVeElBorradorNiSiquieraConHallazgosValidados() throws Exception {
    mockMvc.perform(
        post("/analisis-ia/hallazgos/{id}/validacion", hallazgo(0)).with(como(coordinador)));

    mockMvc
        .perform(get("/analisis-ia/solicitudes/{id}", idSolicitud).with(como(julian)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/expedientes/propio").with(como(julian)))
        .andExpect(jsonPath("$.hallazgos").doesNotExist())
        .andExpect(jsonPath("$.borrador").doesNotExist());
  }

  private long hallazgo(int indice) {
    return ((Number) JsonPath.read(borrador, "$.borrador.hallazgos[" + indice + "].idHallazgo"))
        .longValue();
  }
}
