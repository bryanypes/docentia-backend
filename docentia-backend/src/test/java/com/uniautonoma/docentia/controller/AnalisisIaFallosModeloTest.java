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
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.SolicitudAnalisisRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.analisis.AnalisisIaService;
import com.uniautonoma.docentia.service.analisis.ModeloIA;
import com.uniautonoma.docentia.service.analisis.ModeloIA.ModeloIAException;
import com.uniautonoma.docentia.service.analisis.ModeloIA.RespuestaModelo;
import com.uniautonoma.docentia.service.analisis.ProcesadorSolicitudesAnalisis;
import jakarta.persistence.EntityManager;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** E1: si el modelo responde mal se reintenta una vez; si sigue mal, ERROR con mensaje claro. */
class AnalisisIaFallosModeloTest extends PruebaIntegracion {

  private static final String VALIDO =
      "{\"hallazgos\":[{\"tipo\":\"fortaleza\",\"texto\":\"Clara\",\"citas\":[]}]}";

  @MockitoBean private ModeloIA modelo;

  @Autowired private EscenarioPrueba escenario;
  @Autowired private AnalisisIaService analisisService;
  @Autowired private ProcesadorSolicitudesAnalisis procesador;
  @Autowired private SolicitudAnalisisRepository solicitudRepository;
  @Autowired private EntityManager entityManager;

  private Long idSolicitud;

  @BeforeEach
  void crearSolicitud() {
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    Usuario docente =
        escenario.crearDocente("Julian Hoyos", escenario.crearPrograma("Software", ingenieria));
    Usuario coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    PeriodoAcademico periodo =
        escenario.abrirEtapa(
            escenario.crearPeriodoActivo("2026-2"),
            TipoEtapa.REVISION_HALLAZGOS,
            BaseDatosPruebaConfig.HOY,
            BaseDatosPruebaConfig.HOY);
    escenario.cargarComentarios(
        docente, periodo, coordinador, Collections.nCopies(10, "Explica bien"));
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
  void dosRespuestasInvalidasTerminanEnErrorSinDetallesTecnicos() {
    when(modelo.analizar(any())).thenReturn(new RespuestaModelo("{\"hallazgos\": 3}", "m"));

    SolicitudAnalisis solicitud = procesar();

    assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudAnalisis.ERROR);
    assertThat(solicitud.getMensaje())
        .isEqualTo(
            "No fue posible generar el análisis en este momento. Puedes volver a solicitarlo más"
                + " tarde.");
    assertThat(solicitud.getHallazgos()).isEmpty();
    verify(modelo, times(2)).analizar(any());
  }

  @Test
  void unFalloDelProveedorTerminaEnErrorSinReintentar() {
    when(modelo.analizar(any()))
        .thenThrow(new ModeloIAException("timeout conectando a proveedor:443", null));

    SolicitudAnalisis solicitud = procesar();

    assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudAnalisis.ERROR);
    assertThat(solicitud.getMensaje()).doesNotContain("timeout", "443");
    verify(modelo, times(1)).analizar(any());
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
