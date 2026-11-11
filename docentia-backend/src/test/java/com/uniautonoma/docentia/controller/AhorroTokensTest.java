package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Clase;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.service.analisis.ModeloIA;
import com.uniautonoma.docentia.service.analisis.ModeloIA.PeticionAnalisis;
import com.uniautonoma.docentia.service.analisis.ModeloIA.RespuestaModelo;
import com.uniautonoma.docentia.service.analisis.ModeloIA.Uso;
import com.uniautonoma.docentia.service.analisis.ProcesadorSolicitudesAnalisis;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Ahorro de tokens con el modelo de lenguaje (aquí el simulado, espiado para contar llamadas):
 * caché por huella, presupuesto, citas por id, tendencias desde agregados y anonimato.
 */
@ExtendWith(OutputCaptureExtension.class)
class AhorroTokensTest extends PruebaIntegracion {

  @MockitoSpyBean private ModeloIA modelo;
  @Autowired private EscenarioPrueba escenario;
  @Autowired private ProcesadorSolicitudesAnalisis procesador;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManager entityManager;

  private Programa software;
  private PeriodoAcademico periodo;
  private Usuario docente;
  private Usuario coordinador;
  private Usuario vicerrector;
  private final Map<String, Clase> clases = new HashMap<>();

  @BeforeEach
  void crearEscenario() {
    clearInvocations(modelo);
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    software = escenario.crearPrograma("Ingeniería de Software", ingenieria);
    docente = escenario.crearDocente("Ana María Ruiz", software);
    coordinador = escenario.crearCoordinador("Marta Coordinadora", ingenieria);
    vicerrector = escenario.crearVicerrector("Vicerrectora Uno");
    periodo =
        escenario.crearPeriodoActivoConRecoleccion(
            "2026-2",
            BaseDatosPruebaConfig.HOY.minusDays(20),
            BaseDatosPruebaConfig.HOY.minusDays(1));
    periodo =
        escenario.abrirEtapa(
            periodo,
            TipoEtapa.REVISION_HALLAZGOS,
            BaseDatosPruebaConfig.HOY,
            BaseDatosPruebaConfig.HOY.plusDays(5));
  }

  private void responder(Usuario quien, PeriodoAcademico enPeriodo, List<String> comentarios) {
    String clave = quien.getIdUsuario() + "-" + enPeriodo.getIdPeriodo();
    Clase clase =
        clases.computeIfAbsent(
            clave,
            k ->
                escenario.crearClase(
                    enPeriodo, escenario.crearCurso("C-" + k, "Curso", software), "A", quien));
    Long asignacion =
        jdbcTemplate.queryForObject(
            "select id from clases_docente where clase_id = ?", Long.class, clase.getId());
    for (String comentario : comentarios) {
      jdbcTemplate.update(
          "insert into respuestas_heteroevaluacion"
              + " (asignacion_id, docente_id, periodo_id, instrumento_version, comentario)"
              + " values (?, ?, ?, 'v1', ?)",
          asignacion,
          quien.getIdUsuario(),
          enPeriodo.getIdPeriodo(),
          comentario);
    }
  }

  /**
   * Pide el análisis como lo haría coordinación una hora después de la solicitud anterior: estas
   * pruebas miden la caché y el presupuesto, no el enfriamiento, que tiene su propia prueba.
   */
  private ResultActions solicitar(Usuario quien, PeriodoAcademico enPeriodo, String modeloPedido)
      throws Exception {
    pasarEnfriamiento();
    Map<String, Object> cuerpo = new HashMap<>();
    cuerpo.put("docenteId", quien.getIdUsuario());
    cuerpo.put("periodoId", enPeriodo.getIdPeriodo());
    cuerpo.put("fuente", "FORMULARIO");
    cuerpo.put("motivo", "Repetir el análisis en la prueba");
    if (modeloPedido != null) {
      cuerpo.put("modelo", modeloPedido);
    }
    return mockMvc.perform(
        post("/analisis-ia/solicitudes")
            .with(como(coordinador))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(cuerpo)));
  }

  private void pasarEnfriamiento() {
    entityManager.flush();
    jdbcTemplate.update(
        "update solicitudes_analisis set fecha_solicitud = fecha_solicitud - interval '61 minutes'");
    entityManager.clear();
  }

  private void procesarCola() {
    entityManager.flush();
    entityManager.clear();
    while (procesador.procesarSiguiente()) {
      // todo lo pendiente
    }
    entityManager.flush();
    entityManager.clear();
  }

  private ResultActions analisis(Usuario de) throws Exception {
    return mockMvc.perform(
        get("/analisis-comentarios/docentes/{id}", de.getIdUsuario())
            .param("periodo", periodo.getIdPeriodo().toString())
            .with(como(coordinador)));
  }

  private List<String> usos() {
    return jdbcTemplate.queryForList("select resultado from uso_ia order by id", String.class);
  }

  @Test
  void laMismaEntradaNoVuelveALlamarAlProveedor() throws Exception {
    responder(docente, periodo, AnalisisComentariosTest.COMENTARIOS);
    solicitar(docente, periodo, null).andExpect(status().isAccepted());
    procesarCola();
    analisis(docente)
        .andExpect(jsonPath("$.estado").value("COMPLETADO"))
        .andExpect(jsonPath("$.resultado.tipoModelo").value("LLM"))
        .andExpect(jsonPath("$.resultado.desdeCache").value(false))
        .andExpect(jsonPath("$.resultado.generadoEn").exists())
        .andExpect(jsonPath("$.resultado.limitesAplicados.comentariosAnalizados").value(11));

    solicitar(docente, periodo, null).andExpect(status().isAccepted());
    procesarCola();
    // Abrir la pantalla tampoco llama: las lecturas salen de lo guardado.
    analisis(docente)
        .andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.resultado.desdeCache").value(true));
    mockMvc
        .perform(
            get("/analisis-comentarios/resumen")
                .param("periodo", periodo.getIdPeriodo().toString())
                .with(como(coordinador)))
        .andExpect(status().isOk());

    verify(modelo, times(1)).analizar(any());
    assertThat(usos()).containsExactly("LLAMADA", "ACIERTO_CACHE");
  }

  @Test
  void elegirLlmGastaTokensUnaVezYLuegoSaleDeLaCache() throws Exception {
    mockMvc
        .perform(get("/analisis-comentarios/modelos").with(como(coordinador)))
        .andExpect(jsonPath("$.porDefecto").value("LLM"))
        .andExpect(jsonPath("$.llmDisponible").value(true))
        .andExpect(jsonPath("$.opciones[1].disponible").value(true))
        .andExpect(jsonPath("$.opciones[1].consumeTokens").value(true))
        .andExpect(jsonPath("$.maxComentariosLlm").value(150))
        .andExpect(jsonPath("$.tokensPorAnalisis.totalMaximo").value(20000))
        .andExpect(jsonPath("$.tokensRestantesHoy").value(500000))
        .andExpect(
            jsonPath("$.advertencia").value(org.hamcrest.Matchers.containsString("consume hasta")))
        .andExpect(jsonPath("$.costoEstimado").doesNotExist());
    responder(docente, periodo, AnalisisComentariosTest.COMENTARIOS);
    solicitar(docente, periodo, "LLM").andExpect(status().isAccepted());
    procesarCola();
    solicitar(docente, periodo, "LLM").andExpect(status().isAccepted());
    procesarCola();

    verify(modelo, times(1)).analizar(any());
    assertThat(usos()).containsExactly("LLAMADA", "ACIERTO_CACHE");
    analisis(docente)
        .andExpect(jsonPath("$.resultado.tipoModelo").value("LLM"))
        .andExpect(jsonPath("$.resultado.desdeCache").value(true));
    assertThat(
            jdbcTemplate.queryForList(
                "select modelo_solicitado from solicitudes_analisis order by id_solicitud",
                String.class))
        .containsExactly("LLM", "LLM");
  }

  @Test
  void elegirLlmSinPresupuestoNoLlama() throws Exception {
    jdbcTemplate.update(
        "insert into uso_ia (fecha, id_periodo, proveedor, modelo, version_prompt, huella,"
            + " comentarios_disponibles, comentarios_enviados, tokens_entrada, tokens_salida,"
            + " resultado) values (?, ?, 'simulado', 'x', 'v', ?, 0, 0, 499000, 900, 'LLAMADA')",
        BaseDatosPruebaConfig.HOY.atTime(9, 0),
        periodo.getIdPeriodo(),
        "1".repeat(64));
    responder(docente, periodo, AnalisisComentariosTest.COMENTARIOS);
    solicitar(docente, periodo, "LLM");
    procesarCola();

    analisis(docente).andExpect(jsonPath("$.estado").value("RECHAZADO_PRESUPUESTO"));
    verify(modelo, never()).analizar(any());
  }

  @Test
  void otraEntradaSiLlama() throws Exception {
    responder(docente, periodo, AnalisisComentariosTest.COMENTARIOS);
    solicitar(docente, periodo, null);
    procesarCola();
    responder(docente, periodo, List.of("Las clases virtuales tienen muy mala conexión."));
    solicitar(docente, periodo, null);
    procesarCola();

    verify(modelo, times(2)).analizar(any());
    assertThat(usos()).containsExactly("LLAMADA", "LLAMADA");
  }

  @Test
  void dosSolicitudesSeguidasDanUnaSolaLlamada() throws Exception {
    responder(docente, periodo, AnalisisComentariosTest.COMENTARIOS);
    solicitar(docente, periodo, null).andExpect(status().isAccepted());
    solicitar(docente, periodo, null).andExpect(status().isConflict());
    procesarCola();

    verify(modelo, times(1)).analizar(any());
  }

  @Test
  void dosDocentesConLaMismaEntradaComparten() throws Exception {
    Usuario otro = escenario.crearDocente("Bruno Gómez", software);
    responder(docente, periodo, AnalisisComentariosTest.COMENTARIOS);
    responder(otro, periodo, AnalisisComentariosTest.COMENTARIOS);
    solicitar(docente, periodo, null);
    solicitar(otro, periodo, null);
    procesarCola();

    verify(modelo, times(1)).analizar(any());
  }

  @Test
  void sinPresupuestoNoLlamaYOfreceElModeloLocal() throws Exception {
    jdbcTemplate.update(
        "insert into uso_ia (fecha, id_periodo, proveedor, modelo, version_prompt, huella,"
            + " comentarios_disponibles, comentarios_enviados, tokens_entrada, tokens_salida,"
            + " resultado) values (?, ?, 'simulado', 'x', 'v', ?, 0, 0, 499000, 900, 'LLAMADA')",
        BaseDatosPruebaConfig.HOY.atTime(9, 0),
        periodo.getIdPeriodo(),
        "0".repeat(64));
    responder(docente, periodo, AnalisisComentariosTest.COMENTARIOS);
    solicitar(docente, periodo, null);
    procesarCola();

    analisis(docente)
        .andExpect(jsonPath("$.estado").value("RECHAZADO_PRESUPUESTO"))
        .andExpect(jsonPath("$.motivo").value("PRESUPUESTO_AGOTADO"))
        .andExpect(
            jsonPath("$.mensaje")
                .value(org.hamcrest.Matchers.containsString("modelo local por reglas")));
    verify(modelo, never()).analizar(any());
    assertThat(usos()).endsWith("RECHAZADO_PRESUPUESTO");

    solicitar(docente, periodo, "LOCAL").andExpect(status().isAccepted());
    procesarCola();
    analisis(docente)
        .andExpect(jsonPath("$.estado").value("COMPLETADO"))
        .andExpect(jsonPath("$.resultado.tipoModelo").value("REGLAS"));
    verify(modelo, never()).analizar(any());
  }

  @Test
  void pedirOtroAnalisisPorSolicitudesExigeMotivoYRespetaElEnfriamiento() throws Exception {
    responder(docente, periodo, AnalisisComentariosTest.COMENTARIOS);
    pedir(null).andExpect(status().isAccepted());
    procesarCola();

    pedir(null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.motivo").exists());
    pedir("Se corrigió la carga de respuestas").andExpect(status().isTooManyRequests());

    pasarEnfriamiento();
    pedir("corto").andExpect(status().isBadRequest());
    pedir("Se corrigió la carga de respuestas").andExpect(status().isAccepted());
    assertThat(
            jdbcTemplate.queryForList(
                "select motivo_regeneracion from solicitudes_analisis order by id_solicitud",
                String.class))
        .containsExactly(null, "Se corrigió la carga de respuestas");
  }

  @Test
  void trasUnErrorSePuedeReintentarSinEsperar() throws Exception {
    responder(docente, periodo, AnalisisComentariosTest.COMENTARIOS);
    pedir(null).andExpect(status().isAccepted());
    procesarCola();
    entityManager.flush();
    jdbcTemplate.update("update solicitudes_analisis set estado = 'ERROR'");
    entityManager.clear();

    pedir(null).andExpect(status().isAccepted());
  }

  /** Pedido directo, sin simular el paso del tiempo; motivo nulo = no se envía. */
  private ResultActions pedir(String motivo) throws Exception {
    Map<String, Object> cuerpo = new HashMap<>();
    cuerpo.put("docenteId", docente.getIdUsuario());
    cuerpo.put("periodoId", periodo.getIdPeriodo());
    cuerpo.put("fuente", "FORMULARIO");
    cuerpo.put("modelo", "LOCAL");
    if (motivo != null) {
      cuerpo.put("motivo", motivo);
    }
    return mockMvc.perform(
        post("/analisis-ia/solicitudes")
            .with(como(coordinador))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(cuerpo)));
  }

  @Test
  void regenerarExigeMotivoYRespetaElEnfriamiento() throws Exception {
    responder(docente, periodo, AnalisisComentariosTest.COMENTARIOS);
    String ruta = "/analisis-comentarios/docentes/{id}/regeneracion";
    mockMvc
        .perform(
            post(ruta, docente.getIdUsuario())
                .with(como(vicerrector))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(Map.of("periodoId", periodo.getIdPeriodo(), "fuente", "FORMULARIO"))))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            post(ruta, docente.getIdUsuario())
                .with(como(vicerrector))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        Map.of(
                            "periodoId",
                            periodo.getIdPeriodo(),
                            "fuente",
                            "FORMULARIO",
                            "motivo",
                            "Se corrigió la carga de respuestas"))))
        .andExpect(status().isAccepted());
    procesarCola();
    mockMvc
        .perform(
            post(ruta, docente.getIdUsuario())
                .with(como(coordinador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        Map.of(
                            "periodoId",
                            periodo.getIdPeriodo(),
                            "fuente",
                            "FORMULARIO",
                            "motivo",
                            "Quiero verlo otra vez ahora"))))
        .andExpect(status().isTooManyRequests());
    mockMvc
        .perform(
            post(ruta, docente.getIdUsuario())
                .with(como(docente))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        Map.of(
                            "periodoId",
                            periodo.getIdPeriodo(),
                            "motivo",
                            "Motivo suficiente aquí"))))
        .andExpect(status().isForbidden());
    assertThat(
            jdbcTemplate.queryForObject(
                "select motivo_regeneracion from solicitudes_analisis", String.class))
        .isEqualTo("Se corrigió la carga de respuestas");
  }

  @Test
  void lasCitasPorIdSeResuelvenConElTextoEnviadoYUnIdInexistenteNoVale() throws Exception {
    List<PeticionAnalisis> vistas = new ArrayList<>();
    doAnswer(
            invocacion -> {
              PeticionAnalisis peticion = invocacion.getArgument(0);
              vistas.add(peticion);
              long existente = peticion.comentarios().get(0).id();
              return new RespuestaModelo(
                  "{\"h\":[{\"t\":\"f\",\"x\":\"Explica con claridad\",\"c\":["
                      + existente
                      + "]},{\"t\":\"o\",\"x\":\"Algo inventado\",\"c\":[99999]}],"
                      + "\"r\":\"Resumen breve.\"}",
                  "modelo-de-prueba",
                  new Uso(1200, 80, 0, false));
            })
        .when(modelo)
        .analizar(any());
    responder(docente, periodo, AnalisisComentariosTest.COMENTARIOS);
    long id =
        objectMapper
            .readTree(
                solicitar(docente, periodo, null).andReturn().getResponse().getContentAsString())
            .get("idSolicitud")
            .asLong();
    procesarCola();

    String enviado = vistas.get(0).comentarios().get(0).texto();
    mockMvc
        .perform(get("/analisis-ia/solicitudes/{id}", id).with(como(coordinador)))
        .andExpect(jsonPath("$.borrador.hallazgos[0].citas[0].verificada").value(true))
        .andExpect(jsonPath("$.borrador.hallazgos[0].citas[0].fragmento").value(enviado))
        .andExpect(jsonPath("$.borrador.hallazgos[1].citas[0].verificada").value(false))
        .andExpect(jsonPath("$.borrador.hallazgos[1].estado").value("SIN_CITA_VERIFICADA"));
    analisis(docente).andExpect(jsonPath("$.resultado.resumenNarrativo").value("Resumen breve."));
    assertThat(
            jdbcTemplate.queryForList(
                "select tokens_entrada || '/' || tokens_salida || '/' || estimado from uso_ia",
                String.class))
        .containsExactly("1200/80/false");
    // A los temas del modelo local se suma la muestra: lo que vio el modelo de lenguaje.
    assertThat(vistas.get(0).temas()).isNotEmpty().allMatch(t -> t.matches("D\\d\\d[+~-]\\d+"));
  }

  @Test
  void lasTendenciasSalenDeLosAgregadosSinVolverALosComentariosAnteriores() throws Exception {
    PeriodoAcademico anterior =
        escenario.crearPeriodoCerrado("2026-1", java.time.LocalDate.of(2026, 2, 2));
    escenario.abrirEtapa(
        anterior,
        TipoEtapa.RECOLECCION_CALIFICACIONES,
        java.time.LocalDate.of(2026, 5, 1),
        java.time.LocalDate.of(2026, 5, 30));
    anterior =
        escenario.abrirEtapa(
            anterior,
            TipoEtapa.REVISION_HALLAZGOS,
            BaseDatosPruebaConfig.HOY,
            BaseDatosPruebaConfig.HOY);
    responder(docente, anterior, AnalisisComentariosTest.COMENTARIOS.subList(0, 10));
    solicitar(docente, anterior, "LOCAL");
    procesarCola();
    // Se borran la copia y las respuestas del periodo anterior: solo queda el agregado.
    jdbcTemplate.update("delete from comentarios_analizados");
    jdbcTemplate.update(
        "delete from respuestas_heteroevaluacion where periodo_id = ?", anterior.getIdPeriodo());
    responder(docente, periodo, AnalisisComentariosTest.COMENTARIOS);
    solicitar(docente, periodo, null);
    procesarCola();

    analisis(docente)
        .andExpect(
            jsonPath(
                "$.resultado.tendencias[?(@.cambio != 'SIN_REFERENCIA')]",
                org.hamcrest.Matchers.hasSize(org.hamcrest.Matchers.greaterThan(0))))
        .andExpect(
            jsonPath("$.resultado.limitesAplicados.periodosReferencia[0]")
                .value(anterior.getIdPeriodo()));
  }

  @Test
  void elUsoNoGuardaTextoNiPersonasYNadaVaAlRegistro(CapturedOutput salida) throws Exception {
    responder(docente, periodo, AnalisisComentariosTest.COMENTARIOS);
    solicitar(docente, periodo, null);
    procesarCola();

    List<String> columnas =
        jdbcTemplate.queryForList(
            "select column_name from information_schema.columns where table_name = 'uso_ia'",
            String.class);
    assertThat(columnas)
        .noneMatch(
            c ->
                c.contains("texto")
                    || c.contains("comentario_")
                    || c.contains("docente")
                    || c.contains("usuario")
                    || c.contains("estudiante")
                    || c.contains("respuesta"));
    String filas =
        String.join(
            "\n", jdbcTemplate.queryForList("select uso_ia::text from uso_ia", String.class));
    for (String comentario : AnalisisComentariosTest.COMENTARIOS) {
      String trozo = comentario.substring(0, Math.min(18, comentario.length()));
      assertThat(filas).doesNotContain(trozo);
      assertThat(salida.getAll()).doesNotContain(trozo);
    }
    assertThat(filas).doesNotContain(docente.getNombreCompleto());
    // La copia del formulario sigue sin fechas ni ids de respuesta.
    assertThat(
            jdbcTemplate.queryForList(
                "select column_name from information_schema.columns"
                    + " where table_name = 'comentarios_analizados'",
                String.class))
        .containsExactlyInAnyOrder(
            "id_resultado", "indice", "id_cita", "hash", "texto", "sentimiento", "etiquetas");
  }

  @Test
  void elUsoLoVeSoloVicerrectoria() throws Exception {
    responder(docente, periodo, AnalisisComentariosTest.COMENTARIOS);
    solicitar(docente, periodo, null);
    procesarCola();
    solicitar(docente, periodo, null);
    procesarCola();
    String p = periodo.getIdPeriodo().toString();

    mockMvc
        .perform(get("/analisis-comentarios/uso").param("periodo", p).with(como(vicerrector)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.llamadas").value(1))
        .andExpect(jsonPath("$.aciertosCache").value(1))
        .andExpect(jsonPath("$.ahorroCachePct").value(50.0))
        .andExpect(jsonPath("$.costoEstimado").doesNotExist());
    mockMvc
        .perform(get("/analisis-comentarios/uso").param("periodo", p).with(como(coordinador)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/analisis-comentarios/uso").param("periodo", p).with(como(docente)))
        .andExpect(status().isForbidden());
  }
}
