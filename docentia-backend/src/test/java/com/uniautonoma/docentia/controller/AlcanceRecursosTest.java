package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.FranjaHorariaRepository;
import com.uniautonoma.docentia.repository.SolicitudAnalisisRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.ActaService;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** IDOR: recursos de otra facultad o de otro docente responden 404, no su contenido. */
class AlcanceRecursosTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;
  @Autowired private ActaService actaService;
  @Autowired private FranjaHorariaRepository franjaRepository;
  @Autowired private SolicitudAnalisisRepository solicitudRepository;

  private Usuario docente;
  private Usuario docenteOtraFacultad;
  private Usuario companero;
  private Usuario coordinador;
  private Usuario coordinadorOtraFacultad;
  private PeriodoAcademico periodo;
  private Long idActa;

  @BeforeEach
  void crearEscenario() {
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    Facultad derecho = escenario.crearFacultad("Derecho");
    var software = escenario.crearPrograma("Software", ingenieria);
    docente = escenario.crearDocente("Docente Uno", software);
    companero = escenario.crearDocente("Docente Dos", software);
    docenteOtraFacultad =
        escenario.crearDocente("Docente Tres", escenario.crearPrograma("Leyes", derecho));
    coordinador = escenario.crearCoordinador("Coordinador Uno", ingenieria);
    coordinadorOtraFacultad = escenario.crearCoordinador("Coordinador Dos", derecho);
    periodo =
        escenario.abrirEtapa(
            escenario.crearPeriodoActivo("2026-2"),
            TipoEtapa.AGENDA_CITAS,
            BaseDatosPruebaConfig.HOY,
            BaseDatosPruebaConfig.HOY.plusDays(5));
    idActa = actaService.crear(docente.getIdUsuario(), UsuarioSesion.desde(coordinador)).idActa();
  }

  private int estado(String ruta, Usuario usuario) throws Exception {
    return mockMvc.perform(get(ruta).with(como(usuario))).andReturn().getResponse().getStatus();
  }

  @Test
  void elCoordinadorDeOtraFacultadNoAlcanzaNadaDelDocente() throws Exception {
    Long id = docente.getIdUsuario();
    for (String ruta :
        new String[] {
          "/docentes/" + id,
          "/expedientes/" + id,
          "/docentes/" + id + "/plan-seguimiento",
          "/docentes/" + id + "/calificaciones/correcciones",
          "/docentes/" + id + "/calificaciones/coordinacion",
          "/actas?idDocente=" + id,
          "/actas/" + idActa,
          "/actas/" + idActa + "/pdf",
        }) {
      assertThat(estado(ruta, coordinadorOtraFacultad)).as(ruta).isEqualTo(404);
      assertThat(estado(ruta, coordinador)).as(ruta).isNotEqualTo(403);
    }
  }

  @Test
  void unDocenteNoAlcanzaElActaDeOtro() throws Exception {
    String ruta = "/actas/" + idActa;
    assertThat(estado(ruta, companero)).isEqualTo(404);
    assertThat(estado(ruta + "/pdf", companero)).isEqualTo(404);
    assertThat(estado(ruta, docente)).isEqualTo(200);
  }

  @Test
  void elDocenteSoloListaSusActasAunqueIndiqueOtroId() throws Exception {
    mockMvc
        .perform(
            get("/actas")
                .param("idDocente", docente.getIdUsuario().toString())
                .with(como(companero)))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.length()")
                .value(0));
  }

  @Test
  void unDocenteNoReservaFranjasDeOtraFacultad() throws Exception {
    FranjaHoraria franja =
        franjaRepository.saveAndFlush(
            new FranjaHoraria(
                coordinador, periodo, BaseDatosPruebaConfig.HOY.plusDays(1), LocalTime.of(8, 0)));

    mockMvc
        .perform(
            post("/franjas/{id}/reserva", franja.getIdFranja()).with(como(docenteOtraFacultad)))
        .andExpect(status().isNotFound());
  }

  @Test
  void elAnalisisDeOtraFacultadNoSeConsulta() throws Exception {
    SolicitudAnalisis solicitud =
        solicitudRepository.saveAndFlush(new SolicitudAnalisis(docente, periodo, coordinador));
    String ruta = "/analisis-ia/solicitudes/" + solicitud.getIdSolicitud();

    assertThat(estado(ruta, coordinadorOtraFacultad)).isEqualTo(404);
    assertThat(estado(ruta + "/comentarios-filtrados", coordinadorOtraFacultad)).isEqualTo(404);
    assertThat(
            estado(
                "/analisis-ia/solicitudes?docenteId="
                    + docente.getIdUsuario()
                    + "&periodoId="
                    + periodo.getIdPeriodo(),
                coordinadorOtraFacultad))
        .isEqualTo(404);
  }
}
