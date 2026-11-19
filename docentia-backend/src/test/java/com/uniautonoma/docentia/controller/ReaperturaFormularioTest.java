package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.service.notificacion.EventoNotificacion;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Corrección de un formulario enviado como ticket: lo pide quien lo envió, lo verifica su superior
 * y la administración reabre el formulario.
 */
@RecordApplicationEvents
class ReaperturaFormularioTest extends PruebaIntegracion {

  private static final String AUTO = "/formularios/autoevaluacion";
  private static final String CO = "/formularios/coevaluacion/{id}";
  private static final List<String> COEVAL = List.of("D01", "D06", "D11", "D12", "D13", "D14");

  @Autowired private EscenarioPrueba escenario;
  @Autowired private ApplicationEvents eventos;
  @Autowired private EntityManager entityManager;

  private Facultad ingenieria;
  private Usuario docente;
  private Usuario coordinador;
  private Usuario otroCoordinador;
  private Usuario vicerrector;
  private Usuario administrador;
  private PeriodoAcademico periodo;

  @BeforeEach
  void crearEscenario() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    docente =
        escenario.crearDocente("Docente Uno", escenario.crearPrograma("Software", ingenieria));
    coordinador = escenario.crearCoordinador("Coordinador Uno", ingenieria);
    otroCoordinador =
        escenario.crearCoordinador("Coordinador Dos", escenario.crearFacultad("Salud"));
    vicerrector = escenario.crearVicerrector("Vicerrector Uno");
    administrador = escenario.crearConRol("Administrador Uno", Rol.ADMINISTRADOR);
    periodo =
        escenario.crearPeriodoActivoConRecoleccion(
            "2026-2",
            BaseDatosPruebaConfig.HOY.minusDays(1),
            BaseDatosPruebaConfig.HOY.plusDays(9));
  }

  @Test
  void elDocentePideLaCoordinacionVerificaYLaAdministracionReabre() throws Exception {
    enviarAutoevaluacion(docente, 4);

    long id = idDe(pedir(docente, "AUTO", null).andExpect(status().isCreated()));
    pedir(docente, "AUTO", null)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("abierta")));

    // La coordinación de otra facultad no la ve; la de su facultad sí.
    mockMvc
        .perform(get("/solicitudes-reapertura/por-verificar").with(como(otroCoordinador)))
        .andExpect(jsonPath("$", hasSize(0)));
    mockMvc
        .perform(get("/solicitudes-reapertura/por-verificar").with(como(coordinador)))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].estado").value("PENDIENTE_APROBACION"))
        .andExpect(jsonPath("$[0].rolAprobador").value("COORDINADOR"));
    decidir("verificacion", id, otroCoordinador, true, null).andExpect(status().isNotFound());
    // La administración no la atiende sin la verificación del superior.
    decidir("atencion", id, administrador, true, null).andExpect(status().isConflict());

    decidir("verificacion", id, coordinador, true, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("APROBADA"));
    assertThat(avisosDe(administrador)).contains("REAPERTURA_POR_ATENDER");
    assertThat(avisosDe(docente)).contains("REAPERTURA_APROBADA");

    // Ya cerró la recolección: aun así el formulario reabierto se corrige.
    cerrarRecoleccion();
    decidir("atencion", id, administrador, true, "Reabierto para corregir")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("ATENDIDA"));
    assertThat(avisosDe(docente)).contains("REAPERTURA_ATENDIDA");

    mockMvc
        .perform(get(AUTO).with(como(docente)))
        .andExpect(jsonPath("$.estado").value("BORRADOR"))
        .andExpect(jsonPath("$.ventanaAbierta").value(true))
        .andExpect(jsonPath("$.respondidos").value(14));
    guardar(AUTO, docente, todos(2)).andExpect(status().isOk());
    mockMvc
        .perform(post(AUTO + "/enviar").with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("ENVIADA"));
    // Enviado de nuevo, vuelve a respetar la ventana.
    guardar(AUTO, docente, todos(3)).andExpect(status().isConflict());
  }

  @Test
  void laAutoevaluacionDelCoordinadorYSuCoevaluacionLasVerificaLaVicerrectoria() throws Exception {
    enviarAutoevaluacion(coordinador, 5);
    enviarCoevaluacion(coordinador, docente);

    long auto = idDe(pedir(coordinador, "AUTO", null).andExpect(status().isCreated()));
    long coeval =
        idDe(pedir(coordinador, "COEVAL", docente.getIdUsuario()).andExpect(status().isCreated()));

    mockMvc
        .perform(get("/solicitudes-reapertura/por-verificar").with(como(vicerrector)))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(
            jsonPath("$[*].rolAprobador")
                .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("VICERRECTOR"))));
    decidir("verificacion", auto, coordinador, true, null).andExpect(status().isNotFound());
    decidir("verificacion", coeval, vicerrector, false, "corto").andExpect(status().isBadRequest());
    decidir(
            "verificacion",
            coeval,
            vicerrector,
            false,
            "La coevaluación se hizo con el instrumento vigente")
        .andExpect(jsonPath("$.estado").value("RECHAZADA"))
        .andExpect(jsonPath("$.nombreAprobador").value("Vicerrector Uno"));
    assertThat(avisosDe(coordinador)).contains("REAPERTURA_RECHAZADA");

    mockMvc
        .perform(get("/solicitudes-reapertura/mias").with(como(coordinador)))
        .andExpect(jsonPath("$", hasSize(2)));
    mockMvc
        .perform(get("/solicitudes-reapertura").with(como(administrador)))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].motivo").exists())
        .andExpect(jsonPath("$[0].valor").doesNotExist());
  }

  @Test
  void laCoevaluacionDeLaVicerrectoriaVaDirectoALaAdministracion() throws Exception {
    enviarCoevaluacion(vicerrector, coordinador);

    pedir(vicerrector, "COEVAL", coordinador.getIdUsuario())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("APROBADA"))
        .andExpect(jsonPath("$.rolAprobador").doesNotExist());
    assertThat(avisosDe(administrador)).contains("REAPERTURA_POR_ATENDER");
  }

  @Test
  void soloSePideConElFormularioEnviadoYAntesDeFirmarElActa() throws Exception {
    pedir(docente, "AUTO", null)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("ya enviado")));
    pedir(docente, "AUTO", null).andExpect(status().isConflict());
    mockMvc
        .perform(
            post("/solicitudes-reapertura")
                .with(como(administrador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("tipo", "AUTO", "motivo", "Me equivoqué en varios ítems"))))
        .andExpect(status().isForbidden());
  }

  private void enviarAutoevaluacion(Usuario quien, int valor) throws Exception {
    guardar(AUTO, quien, todos(valor)).andExpect(status().isOk());
    mockMvc.perform(post(AUTO + "/enviar").with(como(quien))).andExpect(status().isOk());
  }

  private void enviarCoevaluacion(Usuario evaluador, Usuario evaluado) throws Exception {
    Map<String, Object> respuestas = new LinkedHashMap<>();
    COEVAL.forEach(codigo -> respuestas.put(codigo, 4));
    mockMvc
        .perform(
            put(CO, evaluado.getIdUsuario())
                .with(como(evaluador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(Map.of("respuestas", respuestas))))
        .andExpect(status().isOk());
    mockMvc
        .perform(post(CO + "/enviar", evaluado.getIdUsuario()).with(como(evaluador)))
        .andExpect(status().isOk());
  }

  private ResultActions guardar(String ruta, Usuario quien, Map<String, Object> respuestas)
      throws Exception {
    return mockMvc.perform(
        put(ruta)
            .with(como(quien))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(Map.of("respuestas", respuestas))));
  }

  private static Map<String, Object> todos(int valor) {
    Map<String, Object> respuestas = new LinkedHashMap<>();
    for (int i = 1; i <= 14; i++) {
      respuestas.put(String.format("D%02d", i), valor);
    }
    return respuestas;
  }

  private ResultActions pedir(Usuario quien, String tipo, Long idDocente) throws Exception {
    Map<String, Object> cuerpo = new LinkedHashMap<>();
    cuerpo.put("tipo", tipo);
    cuerpo.put("idDocente", idDocente);
    cuerpo.put("motivo", "Marqué mal varios ítems del segundo criterio");
    return mockMvc.perform(
        post("/solicitudes-reapertura")
            .with(como(quien))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(cuerpo)));
  }

  private ResultActions decidir(
      String accion, long id, Usuario quien, boolean aprobada, String respuesta) throws Exception {
    Map<String, Object> cuerpo = new LinkedHashMap<>();
    cuerpo.put("aprobada", aprobada);
    cuerpo.put("respuesta", respuesta);
    return mockMvc.perform(
        post("/solicitudes-reapertura/{id}/" + accion, id)
            .with(como(quien))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(cuerpo)));
  }

  private void cerrarRecoleccion() {
    escenario.abrirEtapa(
        entityManager.find(PeriodoAcademico.class, periodo.getIdPeriodo()),
        TipoEtapa.RECOLECCION_CALIFICACIONES,
        BaseDatosPruebaConfig.HOY.minusDays(10),
        BaseDatosPruebaConfig.HOY.minusDays(2));
  }

  /** Los avisos salen al confirmar la transacción; aquí se revisan los eventos publicados. */
  private List<String> avisosDe(Usuario usuario) {
    return eventos.stream(EventoNotificacion.class)
        .filter(
            evento ->
                usuario.getRol() == Rol.ADMINISTRADOR
                    ? evento.destino() == EventoNotificacion.Destino.ADMINISTRADORES
                    : evento.destino() == EventoNotificacion.Destino.USUARIO
                        && evento.idDestino().equals(usuario.getIdUsuario()))
        .map(evento -> evento.tipo().name())
        .toList();
  }

  private static long idDe(ResultActions respuesta) throws Exception {
    String cuerpo = respuesta.andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(cuerpo, "$.idSolicitud")).longValue();
  }
}
