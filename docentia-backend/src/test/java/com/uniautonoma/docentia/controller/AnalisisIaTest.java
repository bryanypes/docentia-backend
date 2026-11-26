package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** E1: solicitud asíncrona del análisis de comentarios con el modelo simulado. */
class AnalisisIaTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;
  @Autowired private ProcesadorSolicitudesAnalisis procesador;
  @Autowired private SolicitudAnalisisRepository solicitudRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

  private Facultad ingenieria;
  private PeriodoAcademico periodo;
  private Usuario julian;
  private Usuario diana;
  private Usuario coordinador;

  @BeforeEach
  void crearEscenario() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    var software = escenario.crearPrograma("Ingeniería de Software", ingenieria);
    julian = escenario.crearDocente("Julian Prueba", software);
    diana = escenario.crearDocente("Diana Ejemplo", software);
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    periodo =
        escenario.abrirEtapa(
            escenario.crearPeriodoActivo("2026-2"),
            TipoEtapa.REVISION_HALLAZGOS,
            BaseDatosPruebaConfig.HOY,
            BaseDatosPruebaConfig.HOY);
    List<String> comentarios = new ArrayList<>();
    comentarios.add("Me llamo Pedro Pérez, escribo desde pedro@x.co. Explica con ejemplos reales");
    for (int i = 1; i <= 11; i++) {
      comentarios.add(i % 2 == 0 ? "Las notas se demoran en el corte " + i : "Domina el tema " + i);
    }
    escenario.cargarComentarios(julian, periodo, coordinador, comentarios);
    escenario.cargarComentarios(
        diana, periodo, coordinador, List.of("Muy clara", "Puntual", "Excelente", "Bien"));
  }

  @Test
  void laSolicitudRespondeDeInmediatoEnColaYElProcesadorDejaElBorrador() throws Exception {
    long id =
        solicitar(julian)
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.estado").value("EN_COLA"))
            .andExpect(jsonPath("$.borrador").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .transform(this::idDe);

    procesarCola();

    mockMvc
        .perform(get("/analisis-ia/solicitudes/{id}", id).with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("COMPLETADO"))
        .andExpect(jsonPath("$.encuestasCerradas").value(12))
        .andExpect(jsonPath("$.comentariosEnviados").value(12))
        .andExpect(jsonPath("$.borrador.modelo").value("simulado-determinista"))
        .andExpect(jsonPath("$.borrador.versionInstruccion").value("analisis-v5"))
        .andExpect(jsonPath("$.borrador.fechaGeneracion").exists())
        .andExpect(jsonPath("$.borrador.estado").value("PENDIENTE_VALIDACION"))
        .andExpect(jsonPath("$.borrador.hallazgos", hasSize(6)))
        .andExpect(
            jsonPath(
                "$.borrador.hallazgos[*].estado",
                everyItem(org.hamcrest.Matchers.is("PENDIENTE_VALIDACION"))))
        .andExpect(
            jsonPath(
                "$.borrador.hallazgos[*].citas[*].verificada",
                everyItem(org.hamcrest.Matchers.is(true))))
        .andExpect(jsonPath("$.borrador.hallazgos[0].tipo").value("FORTALEZA"))
        .andExpect(jsonPath("$.borrador.hallazgos[0].citas[0].idComentario").isNumber())
        .andExpect(
            jsonPath(
                "$.borrador.hallazgos[*].citas[*].fragmento",
                everyItem(not(containsString("Pedro")))));
    // Lo que llegó al modelo ya no tenía el nombre ni el correo del estudiante.
    assertThat(
            jdbcTemplate.queryForList(
                "select c.texto from comentarios_analizados c"
                    + " join resultados_analisis_comentarios r on r.id = c.id_resultado"
                    + " where r.id_solicitud = ?",
                String.class,
                id))
        .contains("Me llamo [nombre], escribo desde [correo]. Explica con ejemplos reales")
        .noneMatch(texto -> texto.contains("Pedro") || texto.contains("pedro@x.co"));
  }

  @Test
  void conMenosDeCincoComentariosNoHayBorradorYElMensajeLoDice() throws Exception {
    long id = idDe(solicitar(diana).andReturn().getResponse().getContentAsString());

    procesarCola();

    mockMvc
        .perform(get("/analisis-ia/solicitudes/{id}", id).with(como(coordinador)))
        .andExpect(jsonPath("$.estado").value("INSUFICIENTE"))
        .andExpect(
            jsonPath("$.mensaje").value("evidencia insuficiente (4 de 5 comentarios requeridos)"))
        .andExpect(jsonPath("$.borrador").doesNotExist());
  }

  @Test
  void conCincoComentariosYaSePuedeAnalizar() throws Exception {
    Usuario camilo = escenario.crearDocente("Camilo Ensayo", julian.getPrograma());
    escenario.cargarComentarios(
        camilo,
        periodo,
        coordinador,
        List.of(
            "Explica con ejemplos reales del sector y se entiende muy bien",
            "Las notas de los parciales se demoran más de dos semanas",
            "Responde las dudas en clase con mucha paciencia",
            "El ritmo de la clase a veces es muy rápido para seguirlo",
            "Los talleres prácticos ayudan a entender los patrones de diseño"));
    long id = idDe(solicitar(camilo).andReturn().getResponse().getContentAsString());

    procesarCola();

    mockMvc
        .perform(get("/analisis-ia/solicitudes/{id}", id).with(como(coordinador)))
        .andExpect(jsonPath("$.estado").value("COMPLETADO"))
        .andExpect(jsonPath("$.comentariosNuevos").value(0));
  }

  @Test
  void conComentariosNuevosAvisaYPermiteActualizarSinEsperar() throws Exception {
    solicitar(julian);
    procesarCola();

    escenario.cargarComentarios(
        julian, periodo, coordinador, List.of("Comentario nuevo uno", "Comentario nuevo dos"));

    mockMvc
        .perform(
            get("/analisis-ia/solicitudes")
                .param("docenteId", julian.getIdUsuario().toString())
                .param("periodoId", periodo.getIdPeriodo().toString())
                .with(como(coordinador)))
        .andExpect(jsonPath("$[0].comentariosNuevos").value(2));
    // Sin comentarios nuevos habría que esperar el enfriamiento de una hora.
    mockMvc
        .perform(
            post("/analisis-ia/solicitudes")
                .with(como(coordinador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        new SolicitudAnalisisRequest(
                            julian.getIdUsuario(),
                            periodo.getIdPeriodo(),
                            null,
                            null,
                            "Llegaron comentarios nuevos de estudiantes"))))
        .andExpect(status().isAccepted());
  }

  @Test
  void noAdmiteDosSolicitudesEnCursoParaElMismoDocenteYPeriodo() throws Exception {
    solicitar(julian).andExpect(status().isAccepted());

    solicitar(julian)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje", containsString("en curso")));

    procesarCola();
    // Con el resultado listo, la siguiente es una regeneración: una hora después y con motivo.
    entityManager.flush();
    jdbcTemplate.update(
        "update solicitudes_analisis set fecha_solicitud = fecha_solicitud - interval '61 minutes'");
    entityManager.clear();
    mockMvc
        .perform(
            post("/analisis-ia/solicitudes")
                .with(como(coordinador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        new SolicitudAnalisisRequest(
                            julian.getIdUsuario(),
                            periodo.getIdPeriodo(),
                            null,
                            null,
                            "Llegaron comentarios nuevos"))))
        .andExpect(status().isAccepted());
    mockMvc
        .perform(
            get("/analisis-ia/solicitudes")
                .param("docenteId", julian.getIdUsuario().toString())
                .param("periodoId", periodo.getIdPeriodo().toString())
                .with(como(coordinador)))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].estado").value("EN_COLA"))
        .andExpect(jsonPath("$[1].estado").value("COMPLETADO"));
  }

  @Test
  void sePuedeSolicitarAunqueLaRevisionDeHallazgosNoHayaAbierto() throws Exception {
    escenario.abrirEtapa(
        periodo,
        TipoEtapa.REVISION_HALLAZGOS,
        BaseDatosPruebaConfig.HOY.plusDays(3),
        BaseDatosPruebaConfig.HOY.plusDays(9));

    solicitar(julian).andExpect(status().isAccepted());
    assertThat(solicitudRepository.count()).isEqualTo(1);
  }

  @Test
  void soloElCoordinadorConAlcanceSolicitaYConsulta() throws Exception {
    Usuario otroCoordinador =
        escenario.crearCoordinador("Paola Patrón", escenario.crearFacultad("Derecho"));
    long id = idDe(solicitar(julian).andReturn().getResponse().getContentAsString());

    solicitar(julian, como(otroCoordinador))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("Docente no encontrado"));
    mockMvc
        .perform(get("/analisis-ia/solicitudes/{id}", id).with(como(otroCoordinador)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("Solicitud no encontrada"));
    // La vicerrectoría también pide análisis: pasa la autorización y choca con la que está en
    // curso.
    solicitar(julian, como(escenario.crearVicerrector("Elena Ficcion")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value(containsString("en curso")));
    solicitar(julian, como(julian)).andExpect(status().isForbidden());
    // El borrador no es visible para el docente en ningún estado.
    mockMvc
        .perform(get("/analisis-ia/solicitudes/{id}", id).with(como(julian)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/analisis-ia/solicitudes/{id}", id).with(comoVicerrector()))
        .andExpect(status().isOk());
  }

  @Test
  void laVicerrectoriaPideAnalisisYRevisaSoloLosDeCoordinadores() throws Exception {
    Usuario vicerrector = escenario.crearVicerrector("Elena Ficcion");
    escenario.cargarComentarios(
        coordinador,
        periodo,
        vicerrector,
        List.of(
            "Explica con ejemplos reales del sector y se entiende muy bien",
            "Las notas de los parciales se demoran más de dos semanas",
            "Responde las dudas en clase con mucha paciencia",
            "El ritmo de la clase a veces es muy rápido para seguirlo",
            "Los talleres prácticos ayudan a entender los patrones de diseño",
            "Explica con ejemplos reales y claros cada tema",
            "Las notas se demoran mucho en publicarse"));
    long deDocente =
        idDe(solicitar(julian, como(vicerrector)).andReturn().getResponse().getContentAsString());
    long deCoordinador =
        idDe(
            solicitar(coordinador, como(vicerrector))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString());
    procesarCola();

    long hallazgoDocente = primerHallazgo(deDocente, vicerrector);
    mockMvc
        .perform(
            post("/analisis-ia/hallazgos/{id}/validacion", hallazgoDocente).with(como(vicerrector)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            post("/analisis-ia/solicitudes/{id}/publicacion", deDocente).with(como(vicerrector)))
        .andExpect(status().isNotFound());

    long hallazgoCoordinador = primerHallazgo(deCoordinador, vicerrector);
    mockMvc
        .perform(
            post("/analisis-ia/hallazgos/{id}/validacion", hallazgoCoordinador)
                .with(como(vicerrector)))
        .andExpect(status().isOk());
  }

  private long primerHallazgo(long idSolicitud, Usuario quien) throws Exception {
    String cuerpo =
        mockMvc
            .perform(get("/analisis-ia/solicitudes/{id}", idSolicitud).with(como(quien)))
            .andExpect(jsonPath("$.estado").value("COMPLETADO"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(cuerpo).at("/borrador/hallazgos/0/idHallazgo").asLong();
  }

  @Test
  void validaElCuerpoYElPeriodo() throws Exception {
    mockMvc
        .perform(
            post("/analisis-ia/solicitudes")
                .with(como(coordinador))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"docenteId\": " + julian.getIdUsuario() + "}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.periodoId").exists());
    mockMvc
        .perform(
            post("/analisis-ia/solicitudes")
                .with(como(coordinador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new SolicitudAnalisisRequest(julian.getIdUsuario(), 999_999L))))
        .andExpect(status().isNotFound());
  }

  @Test
  void unaSolicitudQueQuedoEnProcesoVuelveALaColaAlArrancar() throws Exception {
    long id = idDe(solicitar(julian).andReturn().getResponse().getContentAsString());
    entityManager.flush();
    entityManager
        .createNativeQuery(
            "update solicitudes_analisis set estado = 'EN_PROCESO' where id_solicitud = " + id)
        .executeUpdate();

    assertThat(solicitudRepository.devolverEnProcesoACola()).isEqualTo(1);
    procesarCola();

    assertThat(solicitudRepository.findById(id).orElseThrow().getEstado().name())
        .isEqualTo("COMPLETADO");
  }

  private ResultActions solicitar(Usuario docente) throws Exception {
    return solicitar(docente, como(coordinador));
  }

  private ResultActions solicitar(Usuario docente, RequestPostProcessor quien) throws Exception {
    return mockMvc.perform(
        post("/analisis-ia/solicitudes")
            .with(quien)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                aJson(
                    new SolicitudAnalisisRequest(docente.getIdUsuario(), periodo.getIdPeriodo()))));
  }

  /** Simula una vuelta del procesador en segundo plano, que en producción es otra transacción. */
  private void procesarCola() {
    entityManager.flush();
    entityManager.clear();
    while (procesador.procesarSiguiente()) {
      entityManager.flush();
      entityManager.clear();
    }
  }

  private long idDe(String cuerpo) {
    try {
      return objectMapper.readTree(cuerpo).get("idSolicitud").asLong();
    } catch (Exception excepcion) {
      throw new IllegalStateException(excepcion);
    }
  }
}
