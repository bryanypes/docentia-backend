package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Usuario;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Rol por endpoint. D = docente, C = coordinador, V = vicerrector, E = estudiante, P = público. Si
 * aparece un endpoint nuevo sin clasificar, {@link #todoEndpointEstaClasificado} falla.
 */
class MatrizAutorizacionTest extends PruebaIntegracion {

  private static final Map<String, String> MATRIZ = new TreeMap<>();

  static {
    String[][] filas = {
      {"GET /acciones-sugeridas", "CV"},
      {"POST /actas", "C"},
      {"GET /actas", "DCV"},
      {"GET /actas/{}", "DCV"},
      {"POST /actas/{}/compromisos", "C"},
      {"DELETE /actas/{}/compromisos/{}", "C"},
      {"POST /actas/{}/firmas", "DC"},
      {"POST /actas/{}/correcciones", "V"},
      {"GET /actas/{}/pdf", "DCV"},
      {"POST /analisis-ia/solicitudes", "C"},
      {"GET /analisis-ia/solicitudes", "CV"},
      {"GET /analisis-ia/solicitudes/{}", "CV"},
      {"GET /analisis-ia/solicitudes/{}/comentarios-filtrados", "CV"},
      {"POST /analisis-ia/solicitudes/{}/publicacion", "C"},
      {"GET /analisis-ia/resultados/propio", "D"},
      {"POST /analisis-ia/hallazgos/{}/validacion", "C"},
      {"POST /analisis-ia/hallazgos/{}/descarte", "C"},
      {"PUT /analisis-ia/hallazgos/{}", "C"},
      {"GET /analisis-ia/hallazgos/{}/versiones", "C"},
      {"GET /auditoria", "V"},
      {"GET /autoevaluacion", "D"},
      {"PUT /autoevaluacion", "D"},
      {"GET /docentes/{}/calificaciones/coordinacion", "C"},
      {"PUT /docentes/{}/calificaciones/coordinacion", "C"},
      {"GET /docentes/{}/calificaciones/correcciones", "CV"},
      {"GET /formularios/autoevaluacion", "D"},
      {"PUT /formularios/autoevaluacion", "D"},
      {"POST /formularios/autoevaluacion/enviar", "D"},
      {"GET /formularios/coevaluacion/pendientes", "CV"},
      {"GET /formularios/coevaluacion/{}", "C"},
      {"PUT /formularios/coevaluacion/{}", "C"},
      {"POST /formularios/coevaluacion/{}/enviar", "C"},
      {"GET /estudiante/evaluaciones", "E"},
      {"GET /estudiante/evaluaciones/{}", "E"},
      {"POST /estudiante/evaluaciones/{}/enviar", "E"},
      {"GET /cobertura-heteroevaluacion", "CV"},
      {"POST /matriculas/csv", "CV"},
      {"GET /matriculas/plantilla", "CV"},
      {"GET /franjas/disponibles", "D"},
      {"POST /franjas/{}/reserva", "D"},
      {"GET /citas/mia", "D"},
      {"POST /citas/mia/confirmacion", "D"},
      {"POST /citas/{}/asistencia", "C"},
      {"GET /procesos-evaluacion", "CV"},
      {"GET /procesos-evaluacion/propio", "D"},
      {"GET /procesos-evaluacion/{}", "CV"},
      {"PATCH /actas/{}/tipo", "C"},
      {"GET /configuracion/evaluacion", "DCVE"},
      {"GET /docentes", "CV"},
      {"GET /docentes/seguimiento", "CV"},
      {"GET /docentes/{}", "CV"},
      {"GET /docentes/{}/plan-seguimiento", "CV"},
      {"GET /expedientes/propio", "D"},
      {"GET /expedientes/{}", "CV"},
      {"GET /facultades", "CV"},
      {"GET /facultades/{}", "CV"},
      {"POST /facultades", "V"},
      {"PUT /facultades/{}", "V"},
      {"DELETE /facultades/{}", "V"},
      {"GET /franjas", "C"},
      {"POST /franjas", "C"},
      {"POST /franjas/lote", "C"},
      {"PATCH /franjas/{}/cierre", "C"},
      {"DELETE /franjas/{}", "C"},
      {"POST /importaciones/csv/validar", "C"},
      {"POST /importaciones/csv", "C"},
      {"GET /importaciones", "CV"},
      {"GET /importaciones/plantillas", "C"},
      {"POST /importaciones/plantillas", "C"},
      {"DELETE /importaciones/plantillas/{}", "C"},
      {"GET /kpis", "CV"},
      {"GET /kpis/panel", "CV"},
      {"GET /notificaciones", "DCVE"},
      {"GET /notificaciones/no-leidas/conteo", "DCVE"},
      {"PATCH /notificaciones/{}/leida", "DCVE"},
      {"PATCH /notificaciones/leer-todas", "DCVE"},
      {"GET /notificaciones/preferencias", "DCVE"},
      {"PUT /notificaciones/preferencias", "DCVE"},
      {"GET /periodos-academicos", "DCVE"},
      {"GET /periodos-academicos/activo", "DCVE"},
      {"GET /periodos-academicos/activo/calendario", "DCVE"},
      {"GET /periodos-academicos/activo/ventanas", "DCVE"},
      {"GET /periodos-academicos/{}", "DCVE"},
      {"POST /periodos-academicos", "V"},
      {"PUT /periodos-academicos/{}", "V"},
      {"PUT /periodos-academicos/{}/activacion", "V"},
      {"DELETE /periodos-academicos/{}/activacion", "V"},
      {"GET /periodos-academicos/activo/ponderaciones", "CV"},
      {"PUT /periodos-academicos/activo/ponderaciones", "V"},
      {"GET /plan-seguimiento/propio", "D"},
      {"PUT /compromisos/{}/cumplimiento", "C"},
      {"GET /programas", "CV"},
      {"GET /programas/{}", "CV"},
      {"POST /programas", "V"},
      {"PUT /programas/{}", "V"},
      {"DELETE /programas/{}", "V"},
      {"POST /sesiones", "P"},
      {"POST /sesiones/google", "P"},
      {"GET /sesiones/actual", "DCVE"},
      {"PUT /sesiones/actual/contrasena", "DCVE"},
      {"GET /usuarios", "V"},
      {"GET /usuarios/{}", "V"},
      {"POST /usuarios", "V"},
      {"PUT /usuarios/{}", "V"},
      {"PATCH /usuarios/{}/estado", "V"},
      {"GET /verificacion-actas/{}", "P"},
    };
    for (String[] fila : filas) {
      MATRIZ.put(fila[0], fila[1]);
    }
  }

  @Autowired private EscenarioPrueba escenario;

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping mapeos;

  private Usuario docente;
  private Usuario coordinador;
  private Usuario vicerrector;
  private Usuario estudiante;

  @BeforeEach
  void crearEscenario() {
    Facultad facultad = escenario.crearFacultad("Ingeniería");
    Programa programa = escenario.crearPrograma("Software", facultad);
    docente = escenario.crearDocente("Docente Uno", programa);
    estudiante = escenario.crearEstudiante("Estudiante Uno", programa);
    coordinador = escenario.crearCoordinador("Coordinador Uno", facultad);
    vicerrector = escenario.crearVicerrector("Vicerrector Uno");
  }

  static Stream<Arguments> casos() {
    return MATRIZ.entrySet().stream()
        .flatMap(
            fila ->
                Stream.of('D', 'C', 'V', 'E')
                    .map(rol -> Arguments.of(fila.getKey(), fila.getValue(), rol)));
  }

  static Stream<String> endpoints() {
    return MATRIZ.keySet().stream();
  }

  private MockHttpServletRequestBuilder peticion(String endpoint) {
    String[] partes = endpoint.split(" ");
    return request(HttpMethod.valueOf(partes[0]), partes[1].replace("{}", "987654"))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}");
  }

  @Test
  void todoEndpointEstaClasificado() {
    Set<String> reales = new TreeSet<>();
    mapeos
        .getHandlerMethods()
        .forEach(
            (info, metodo) -> {
              if (!metodo.getBeanType().getPackageName().endsWith(".controller")) {
                return;
              }
              info.getMethodsCondition()
                  .getMethods()
                  .forEach(
                      verbo ->
                          info.getPathPatternsCondition()
                              .getPatternValues()
                              .forEach(
                                  ruta ->
                                      reales.add(
                                          verbo + " " + ruta.replaceAll("\\{[^}]+}", "{}"))));
            });
    assertThat(reales).containsExactlyInAnyOrderElementsOf(MATRIZ.keySet());
  }

  @ParameterizedTest(name = "{0} con rol {2}")
  @MethodSource("casos")
  void cadaRolRecibeLoQueLeToca(String endpoint, String permitidos, char rol) throws Exception {
    if (permitidos.equals("P")) {
      return;
    }
    Usuario usuario =
        switch (rol) {
          case 'D' -> docente;
          case 'C' -> coordinador;
          case 'V' -> vicerrector;
          default -> estudiante;
        };
    RequestPostProcessor sesion = como(usuario);
    int estado =
        mockMvc.perform(peticion(endpoint).with(sesion)).andReturn().getResponse().getStatus();

    if (permitidos.indexOf(rol) >= 0) {
      assertThat(estado).as("%s para %s", endpoint, rol).isNotIn(401, 403);
    } else {
      // Bean Validation corre antes que el control de método: un 400 o un 415 también son rechazo.
      assertThat(estado).as("%s para %s", endpoint, rol).isIn(400, 403, 415);
    }
  }

  @ParameterizedTest(name = "{0} sin sesión")
  @MethodSource("endpoints")
  void sinSesionResponde401SalvoLoPublico(String endpoint) throws Exception {
    int estado = mockMvc.perform(peticion(endpoint)).andReturn().getResponse().getStatus();
    if (MATRIZ.get(endpoint).equals("P")) {
      assertThat(estado).isNotEqualTo(401);
    } else {
      assertThat(estado).as(endpoint).isEqualTo(401);
    }
  }
}
