package com.uniautonoma.docentia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.PreferenciasNotificacionRequest;
import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.TipoNotificacion;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CitaRepository;
import com.uniautonoma.docentia.repository.CompromisoRepository;
import com.uniautonoma.docentia.repository.FranjaHorariaRepository;
import com.uniautonoma.docentia.service.notificacion.EnviadorCorreo;
import com.uniautonoma.docentia.service.notificacion.EventoNotificacion;
import com.uniautonoma.docentia.service.notificacion.GeneradorRecordatorios;
import com.uniautonoma.docentia.service.notificacion.NotificacionService;
import com.uniautonoma.docentia.service.notificacion.ProcesadorColaCorreo;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Sin transacción de prueba: los avisos solo existen después de un commit real. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificacionesTest extends PruebaIntegracion {

  private static final String MARCA = "Notificaciones Prueba";
  private static final String PERIODO = "NT-2026";

  @Autowired private EscenarioPrueba escenario;
  @Autowired private NotificacionService notificaciones;
  @Autowired private ProcesadorColaCorreo colaCorreo;
  @Autowired private GeneradorRecordatorios recordatorios;
  @Autowired private ApplicationEventPublisher eventos;
  @Autowired private TransactionTemplate transaccion;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private CompromisoRepository compromisoRepository;
  @Autowired private FranjaHorariaRepository franjaRepository;
  @Autowired private CitaRepository citaRepository;
  @MockitoBean private EnviadorCorreo enviador;

  private Usuario docente;
  private Usuario otroDocente;
  private Usuario coordinador;
  private Usuario coordinadorOtraFacultad;
  private Usuario vicerrector;
  private PeriodoAcademico periodo;

  @BeforeEach
  void crearEscenario() {
    transaccion.executeWithoutResult(
        estado -> {
          Facultad facultad = escenario.crearFacultad("Facultad " + MARCA);
          Facultad otra = escenario.crearFacultad("Otra Facultad " + MARCA);
          docente =
              escenario.crearDocente(
                  "Docente " + MARCA, escenario.crearPrograma("Programa " + MARCA, facultad));
          otroDocente =
              escenario.crearDocente(
                  "Otro Docente " + MARCA, escenario.crearPrograma("Otro Programa " + MARCA, otra));
          coordinador = escenario.crearCoordinador("Coordinador " + MARCA, facultad);
          coordinadorOtraFacultad = escenario.crearCoordinador("Coordinador B " + MARCA, otra);
          vicerrector = escenario.crearVicerrector("Vicerrector " + MARCA);
          periodo =
              escenario.abrirEtapa(
                  escenario.crearPeriodoActivo(PERIODO),
                  TipoEtapa.AGENDA_CITAS,
                  BaseDatosPruebaConfig.HOY.minusDays(1),
                  BaseDatosPruebaConfig.HOY.plusDays(2));
          periodo =
              escenario.abrirEtapa(
                  periodo,
                  TipoEtapa.RECOLECCION_CALIFICACIONES,
                  BaseDatosPruebaConfig.HOY.minusDays(1),
                  BaseDatosPruebaConfig.HOY.plusDays(2));
        });
  }

  @AfterEach
  void borrarLoCreado() {
    reset(enviador);
    jdbcTemplate.update(
        "delete from citas where id_periodo in"
            + " (select id_periodo from periodos_academicos where nombre = ?)",
        PERIODO);
    jdbcTemplate.update(
        "delete from franjas_horarias where id_periodo in"
            + " (select id_periodo from periodos_academicos where nombre = ?)",
        PERIODO);
    jdbcTemplate.update(
        "delete from compromisos where id_periodo in"
            + " (select id_periodo from periodos_academicos where nombre = ?)",
        PERIODO);
    jdbcTemplate.update("delete from periodos_academicos where nombre = ?", PERIODO);
    jdbcTemplate.update("delete from usuarios where nombre_completo like ?", "%" + MARCA + "%");
    jdbcTemplate.update("delete from programas where nombre like ?", "%" + MARCA + "%");
    jdbcTemplate.update("delete from facultades where nombre like ?", "%" + MARCA + "%");
    jdbcTemplate.update("delete from cola_correo where notificacion_id is null");
  }

  private EventoNotificacion alDocente(String clave) {
    return EventoNotificacion.aUsuario(
        TipoNotificacion.ACTA_POR_FIRMAR, docente.getIdUsuario(), "/mi-acta", "ACTA", 1L, clave);
  }

  private int avisos(Usuario usuario) {
    return jdbcTemplate.queryForObject(
        "select count(*) from notificaciones where usuario_id = ?",
        Integer.class,
        usuario.getIdUsuario());
  }

  private int correos(Usuario usuario, String estado) {
    return jdbcTemplate.queryForObject(
        "select count(*) from cola_correo c join notificaciones n on n.id = c.notificacion_id"
            + " where n.usuario_id = ? and c.estado = ?",
        Integer.class,
        usuario.getIdUsuario(),
        estado);
  }

  private void adelantarCola() {
    jdbcTemplate.update(
        "update cola_correo set proximo_intento = '2000-01-01' where estado = 'PENDIENTE'");
  }

  @Test
  void elMismoSucesoNoSeDuplica() {
    notificaciones.notificar(alDocente("acta:1"));
    notificaciones.notificar(alDocente("acta:1"));

    assertThat(avisos(docente)).isEqualTo(1);
    assertThat(correos(docente, "PENDIENTE")).isEqualTo(1);
  }

  @Test
  void siLaTransaccionSeRevierteNoSeNotifica() {
    transaccion.executeWithoutResult(
        estado -> {
          eventos.publishEvent(alDocente("acta:revertida"));
          estado.setRollbackOnly();
        });
    assertThat(avisos(docente)).isZero();

    transaccion.executeWithoutResult(estado -> eventos.publishEvent(alDocente("acta:confirmada")));
    assertThat(avisos(docente)).isEqualTo(1);
  }

  @Test
  void elCoordinadorSoloRecibeLoDeSuFacultad() {
    transaccion.executeWithoutResult(
        estado -> {
          Usuario enSesion = escenario.recargar(docente);
          eventos.publishEvent(
              EventoNotificacion.aCoordinadores(
                  TipoNotificacion.ACTA_FIRMADA_DOCENTE, enSesion, "/x", "ACTA", 1L, "acta:1"));
        });

    assertThat(avisos(coordinador)).isEqualTo(1);
    assertThat(avisos(coordinadorOtraFacultad)).isZero();
    assertThat(avisos(vicerrector)).isZero();
    assertThat(avisos(docente)).isZero();
  }

  @Test
  void laAlertaInformativaSoloLlegaAlVicerrectorYNoAlDocente() {
    notificaciones.notificar(
        EventoNotificacion.aVicerrectores(
            TipoNotificacion.ALERTA_INFORMATIVA, "/acciones-sugeridas", "PERIODO", 1L, "d:1"));

    assertThat(avisos(vicerrector)).isEqualTo(1);
    assertThat(avisos(docente)).isZero();
    assertThat(avisos(coordinador)).isZero();
  }

  @Test
  void leerUnAvisoAjenoResponde404YElPropioFunciona() throws Exception {
    notificaciones.notificar(alDocente("acta:1"));
    Long id =
        jdbcTemplate.queryForObject(
            "select id from notificaciones where usuario_id = ?",
            Long.class,
            docente.getIdUsuario());

    mockMvc
        .perform(patch("/notificaciones/{id}/leida", id).with(como(otroDocente)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/notificaciones/no-leidas/conteo").with(como(docente)))
        .andExpect(jsonPath("$.noLeidas").value(1));
    mockMvc
        .perform(patch("/notificaciones/{id}/leida", id).with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leida").value(true));
    mockMvc
        .perform(get("/notificaciones").param("soloNoLeidas", "true").with(como(docente)))
        .andExpect(jsonPath("$.totalElementos").value(0));
    mockMvc.perform(get("/notificaciones")).andExpect(status().isUnauthorized());
  }

  @Test
  void elTamanoDePaginaTieneTope() throws Exception {
    mockMvc
        .perform(get("/notificaciones").param("size", "500").with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tamano").value(50));
  }

  @Test
  void leerTodasSoloTocaLasPropias() throws Exception {
    notificaciones.notificar(alDocente("acta:1"));
    notificaciones.notificar(
        EventoNotificacion.aUsuario(
            TipoNotificacion.ACTA_POR_FIRMAR, otroDocente.getIdUsuario(), "/x", "ACTA", 2L, "a"));

    mockMvc
        .perform(patch("/notificaciones/leer-todas").with(como(docente)))
        .andExpect(status().isNoContent());

    assertThat(notificaciones.contarNoLeidas(docente.getIdUsuario())).isZero();
    assertThat(notificaciones.contarNoLeidas(otroDocente.getIdUsuario())).isEqualTo(1);
  }

  @Test
  void sinCorreoHabilitadoQuedaElAvisoPeroNoElCorreo() throws Exception {
    mockMvc
        .perform(
            put("/notificaciones/preferencias")
                .with(como(docente))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new PreferenciasNotificacionRequest(false))))
        .andExpect(jsonPath("$.correoHabilitado").value(false));

    notificaciones.notificar(alDocente("acta:1"));

    assertThat(avisos(docente)).isEqualTo(1);
    assertThat(correos(docente, "PENDIENTE")).isZero();
  }

  @Test
  void elCorreoSeReintentaHastaQuedarEnError() {
    doThrow(new MailSendException("smtp caído para persona@correo.co"))
        .when(enviador)
        .enviar(any(), any(), any());
    notificaciones.notificar(alDocente("acta:1"));

    for (int i = 0; i < 4; i++) {
      adelantarCola();
      colaCorreo.procesarLote();
      assertThat(correos(docente, "PENDIENTE")).isEqualTo(1);
    }
    adelantarCola();
    colaCorreo.procesarLote();

    assertThat(correos(docente, "ERROR")).isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select ultimo_error from cola_correo c join notificaciones n"
                    + " on n.id = c.notificacion_id where n.usuario_id = ?",
                String.class,
                docente.getIdUsuario()))
        .isEqualTo("MailSendException");
  }

  @Test
  void unCorreoEnviadoNoSeReenvia() {
    notificaciones.notificar(alDocente("acta:1"));

    colaCorreo.procesarLote();
    assertThat(correos(docente, "ENVIADO")).as("sale en la primera pasada").isEqualTo(1);
    adelantarCola();
    colaCorreo.procesarLote();

    assertThat(correos(docente, "ENVIADO")).isEqualTo(1);
    org.mockito.Mockito.verify(enviador, org.mockito.Mockito.times(1)).enviar(any(), any(), any());
  }

  @Test
  void unFalloAlNotificarNoLlegaAlNegocio() {
    assertThatCode(
            () ->
                notificaciones.notificar(
                    new EventoNotificacion(
                        TipoNotificacion.ACTA_FIRMADA,
                        EventoNotificacion.Destino.USUARIO,
                        docente.getIdUsuario(),
                        "/x".repeat(200),
                        "ACTA",
                        1L,
                        "muy-largo",
                        null)))
        .doesNotThrowAnyException();
    assertThat(avisos(docente)).isZero();
  }

  @ParameterizedTest
  @EnumSource(TipoNotificacion.class)
  void ningunCorreoLlevaDatosSensibles(TipoNotificacion tipo) {
    notificaciones.notificar(
        EventoNotificacion.aUsuario(
                tipo, docente.getIdUsuario(), "/mi-acta", "ACTA", 1L, "c:" + tipo)
            .conDetalle("Código de verificación: DOC-47E0085BC21A."));

    String correo =
        jdbcTemplate.queryForObject(
            "select asunto || ' ' || cuerpo from cola_correo c join notificaciones n"
                + " on n.id = c.notificacion_id where n.usuario_id = ?",
            String.class,
            docente.getIdUsuario());
    assertThat(correo)
        .doesNotContainPattern("\\b[0-5][.,][0-9]")
        .doesNotContainPattern("(?i)desempeño|muy bajo|muy alto|desvincul|fortaleza|comentario de")
        .doesNotContain("<img", "http://cdn");
    assertThat(correo).contains("Corporación Universitaria Autónoma del Cauca");
  }

  @Test
  void losFormulariosPendientesSeRecuerdanSinPuntajesNiDuplicados() {
    recordatorios.generar();
    recordatorios.generar();

    assertThat(tiposDe(docente).stream().filter("AUTOEVALUACION_PENDIENTE"::equals)).hasSize(1);
    assertThat(tiposDe(coordinador).stream().filter("COEVALUACIONES_PENDIENTES"::equals))
        .hasSize(1);
    String cuerpo =
        jdbcTemplate.queryForObject(
            "select c.cuerpo from cola_correo c join notificaciones n on n.id = c.notificacion_id"
                + " where n.usuario_id = ? and n.tipo = 'COEVALUACIONES_PENDIENTES'",
            String.class,
            coordinador.getIdUsuario());
    assertThat(cuerpo).contains("Pendientes: 1.").doesNotContain("Docente " + MARCA);
  }

  @Test
  void laAperturaSeAvisaAlAbrirYNoDiasDespues() {
    recordatorios.generar();
    assertThat(tiposDe(docente)).contains("VENTANA_ABIERTA");
    assertThat(tiposDe(coordinador)).contains("VENTANA_ABIERTA");

    jdbcTemplate.update("delete from notificaciones");
    jdbcTemplate.update(
        "update etapas_periodo set fecha_apertura = fecha_apertura - 10 where id_periodo = ?",
        periodo.getIdPeriodo());
    recordatorios.generar();
    assertThat(tiposDe(docente)).doesNotContain("VENTANA_ABIERTA");
  }

  private List<String> tiposDe(Usuario usuario) {
    return jdbcTemplate.queryForList(
        "select tipo from notificaciones where usuario_id = ?",
        String.class,
        usuario.getIdUsuario());
  }

  @Test
  void losRecordatoriosNoSeDuplicanAunqueCorranDosVeces() {
    transaccion.executeWithoutResult(
        estado -> {
          Usuario enSesion = escenario.recargar(docente);
          compromisoRepository.save(
              new Compromiso(
                  enSesion, periodo, "Vence pronto", BaseDatosPruebaConfig.HOY.plusDays(5)));
          compromisoRepository.save(
              new Compromiso(
                  enSesion, periodo, "Ya venció", BaseDatosPruebaConfig.HOY.minusDays(1)));
          FranjaHoraria franja =
              franjaRepository.saveAndFlush(
                  new FranjaHoraria(
                      escenario.recargar(coordinador),
                      periodo,
                      BaseDatosPruebaConfig.HOY.plusDays(1),
                      LocalTime.of(8, 0)));
          citaRepository.saveAndFlush(new Cita(franja, enSesion));
        });

    recordatorios.generar();
    int docenteUna = avisos(docente);
    int coordinadorUna = avisos(coordinador);
    recordatorios.generar();

    assertThat(avisos(docente)).isEqualTo(docenteUna);
    assertThat(avisos(coordinador)).isEqualTo(coordinadorUna);
    List<String> tipos =
        jdbcTemplate.queryForList(
            "select tipo from notificaciones where usuario_id = ?",
            String.class,
            docente.getIdUsuario());
    assertThat(tipos).contains("RECORDATORIO_CITA", "COMPROMISO_POR_VENCER", "COMPROMISO_VENCIDO");
    assertThat(avisos(coordinadorOtraFacultad))
        .as("la otra facultad no recibe el vencido ajeno")
        .isLessThan(coordinadorUna);
  }
}
