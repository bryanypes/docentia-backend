package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.SolicitudAnalisisRequest;
import com.uniautonoma.docentia.entity.EstadoSolicitudAnalisis;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.TipoNotificacion;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.SolicitudAnalisisRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.analisis.AnalisisIaService;
import com.uniautonoma.docentia.service.analisis.ModeloIA;
import com.uniautonoma.docentia.service.analisis.ModeloIA.ClaseFallo;
import com.uniautonoma.docentia.service.analisis.ModeloIA.ModeloIAException;
import com.uniautonoma.docentia.service.analisis.ModeloIA.PeticionAnalisis;
import com.uniautonoma.docentia.service.analisis.ModeloIA.RespuestaModelo;
import com.uniautonoma.docentia.service.analisis.ProcesadorSolicitudesAnalisis;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorLocal;
import com.uniautonoma.docentia.service.notificacion.EventoNotificacion;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Fallos del modelo externo: cada clase tiene su tratamiento (reintento con espera, mitad de los
 * comentarios, sin reintento) y, si no se logra, el análisis lo hace el modelo local, rotulado. Un
 * fallo del proveedor no deja la solicitud en ERROR.
 */
@RecordApplicationEvents
class AnalisisIaFallosModeloTest extends PruebaIntegracion {

  private static final String VALIDO =
      "{\"hallazgos\":[{\"tipo\":\"fortaleza\",\"texto\":\"Clara\",\"citas\":[]}]}";

  @MockitoBean private ModeloIA modelo;

  @Autowired private EscenarioPrueba escenario;
  @Autowired private AnalisisIaService analisisService;
  @Autowired private ProcesadorSolicitudesAnalisis procesador;
  @Autowired private SolicitudAnalisisRepository solicitudRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private ApplicationEvents eventos;

  private Long idSolicitud;

  @BeforeEach
  void crearSolicitud() {
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    Usuario docente =
        escenario.crearDocente("Julian Prueba", escenario.crearPrograma("Software", ingenieria));
    Usuario coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    PeriodoAcademico periodo =
        escenario.abrirEtapa(
            escenario.crearPeriodoActivo("2026-2"),
            TipoEtapa.REVISION_HALLAZGOS,
            BaseDatosPruebaConfig.HOY,
            BaseDatosPruebaConfig.HOY);
    escenario.cargarComentarios(
        docente,
        periodo,
        coordinador,
        java.util.stream.IntStream.rangeClosed(1, 10)
            .mapToObj(i -> "Explica bien el tema " + i)
            .toList());
    idSolicitud =
        analisisService
            .solicitar(
                new SolicitudAnalisisRequest(docente.getIdUsuario(), periodo.getIdPeriodo()),
                UsuarioSesion.desde(coordinador))
            .idSolicitud();
  }

  @Test
  void unJsonInvalidoSeReintentaUnaVez() {
    when(modelo.analizar(any()))
        .thenReturn(new RespuestaModelo("No es JSON", "m"))
        .thenReturn(new RespuestaModelo(VALIDO, "modelo-real"));

    SolicitudAnalisis solicitud = procesar();

    assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudAnalisis.COMPLETADO);
    assertThat(solicitud.getModelo()).isEqualTo("modelo-real");
    verify(modelo, times(2)).analizar(any());
  }

  @Test
  void dosRespuestasInvalidasUsanElModeloLocalSinDetallesTecnicos() {
    when(modelo.analizar(any())).thenReturn(new RespuestaModelo("{\"hallazgos\": 3}", "m"));

    SolicitudAnalisis solicitud = procesar();

    assertRespaldoLocal(solicitud, "RESPUESTA_INVALIDA");
    assertThat(solicitud.getMensaje()).contains("no tuvo el formato esperado");
    verify(modelo, times(2)).analizar(any());
  }

  @Test
  void unFalloPasajeroSeReintentaYLuegoUsaElModeloLocal() {
    when(modelo.analizar(any()))
        .thenThrow(new ModeloIAException("timeout conectando a proveedor:443", null));

    SolicitudAnalisis solicitud = procesar();

    assertRespaldoLocal(solicitud, "PROVEEDOR_NO_DISPONIBLE");
    assertThat(solicitud.getMensaje()).doesNotContain("timeout", "443");
    // Tres llamadas: la primera y dos reintentos.
    verify(modelo, times(3)).analizar(any());
  }

  @Test
  void un429SeguidoDeRespuestaValidaUsaElModeloExterno() {
    when(modelo.analizar(any()))
        .thenThrow(
            new ModeloIAException(
                "El proveedor respondió HTTP 429",
                null,
                ClaseFallo.TRANSITORIO,
                Duration.ZERO,
                null))
        .thenReturn(new RespuestaModelo(VALIDO, "modelo-real"));

    SolicitudAnalisis solicitud = procesar();

    assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudAnalisis.COMPLETADO);
    assertThat(solicitud.getModelo()).isEqualTo("modelo-real");
    assertThat(solicitud.getMotivo()).isNull();
    verify(modelo, times(2)).analizar(any());
  }

  @Test
  void unRechazoDeConfiguracionNoSeReintentaUsaElLocalYAvisa() {
    when(modelo.analizar(any()))
        .thenThrow(
            new ModeloIAException(
                "El proveedor respondió HTTP 400", null, ClaseFallo.CONFIGURACION));

    SolicitudAnalisis solicitud = procesar();

    assertRespaldoLocal(solicitud, "PROVEEDOR_CONFIGURACION");
    assertThat(solicitud.getMensaje()).contains("La administración ya fue avisada");
    verify(modelo, times(1)).analizar(any());
    // Un aviso a la administración y otro a la vicerrectoría.
    assertThat(
            eventos.stream(EventoNotificacion.class)
                .filter(e -> e.tipo() == TipoNotificacion.PROVEEDOR_IA_RECHAZO))
        .hasSize(2);
  }

  @Test
  void unaRespuestaCortadaSeRepiteConLaMitadDeLosComentarios() {
    when(modelo.analizar(any()))
        .thenReturn(new RespuestaModelo("{\"hallazgos\":[{\"tipo\":\"fortaleza\",\"tex", "m"))
        .thenReturn(new RespuestaModelo(VALIDO, "modelo-real"));
    ArgumentCaptor<PeticionAnalisis> peticiones = ArgumentCaptor.forClass(PeticionAnalisis.class);

    SolicitudAnalisis solicitud = procesar();

    verify(modelo, times(2)).analizar(peticiones.capture());
    int primera = peticiones.getAllValues().get(0).comentarios().size();
    int segunda = peticiones.getAllValues().get(1).comentarios().size();
    assertThat(segunda).isEqualTo((primera + 1) / 2);
    assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudAnalisis.COMPLETADO);
    assertThat(solicitud.getModelo()).isEqualTo("modelo-real");
    assertThat(solicitud.getComentariosEnviados()).isEqualTo(segunda);
  }

  @Test
  void dosRespuestasCortadasUsanElModeloLocal() {
    when(modelo.analizar(any()))
        .thenThrow(
            new ModeloIAException(
                "El proveedor cortó la respuesta por el tope de tokens",
                null,
                ClaseFallo.CORTADA,
                null,
                new ModeloIA.Uso(900, 8000, 0, false)));

    SolicitudAnalisis solicitud = procesar();

    assertRespaldoLocal(solicitud, "RESPUESTA_CORTADA");
    verify(modelo, times(2)).analizar(any());
  }

  @Test
  void elJsonConTextoAntesYDespuesDelBloqueSeAcepta() {
    when(modelo.analizar(any()))
        .thenReturn(
            new RespuestaModelo(
                "Aquí está el análisis:\n```json\n" + VALIDO + "\n```\nQuedo atento.",
                "modelo-real"));

    SolicitudAnalisis solicitud = procesar();

    assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudAnalisis.COMPLETADO);
    assertThat(solicitud.getModelo()).isEqualTo("modelo-real");
    verify(modelo, times(1)).analizar(any());
  }

  @Test
  void unErrorInesperadoDelProveedorTambienUsaElModeloLocal() {
    when(modelo.analizar(any())).thenThrow(new IllegalStateException("algo raro"));

    SolicitudAnalisis solicitud = procesar();

    assertRespaldoLocal(solicitud, "PROVEEDOR_NO_DISPONIBLE");
    verify(modelo, times(1)).analizar(any());
  }

  /** Completado por el modelo local, con el motivo guardado y una explicación para el usuario. */
  private static void assertRespaldoLocal(SolicitudAnalisis solicitud, String motivo) {
    assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudAnalisis.COMPLETADO);
    assertThat(solicitud.getMotivo()).isEqualTo(motivo);
    assertThat(solicitud.getModelo()).isEqualTo(AnalizadorLocal.VERSION);
    assertThat(solicitud.getMensaje()).contains("modelo local por reglas");
  }

  private SolicitudAnalisis procesar() {
    entityManager.flush();
    entityManager.clear();
    assertThat(procesador.procesarSiguiente()).isTrue();
    entityManager.flush();
    entityManager.clear();
    return solicitudRepository.findById(idSolicitud).orElseThrow();
  }
}
