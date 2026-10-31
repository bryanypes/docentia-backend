package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.FranjaRequest;
import com.uniautonoma.docentia.dto.LoteFranjasRequest;
import com.uniautonoma.docentia.dto.LoteFranjasRequest.AccionLote;
import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import com.uniautonoma.docentia.entity.MotivoLiberacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CitaRepository;
import com.uniautonoma.docentia.repository.FranjaHorariaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

/** F1: franjas de 30 minutos del coordinador, sin solapes y sin tocar las reservadas. */
class FranjaTest extends PruebaIntegracion {

  private static final LocalDate DIA = BaseDatosPruebaConfig.HOY.plusDays(7);

  @Autowired private EscenarioPrueba escenario;
  @Autowired private FranjaHorariaRepository franjaRepository;
  @Autowired private CitaRepository citaRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private PeriodoAcademico periodo;
  private Usuario coordinador;
  private Usuario otroCoordinador;
  private Usuario docente;

  @BeforeEach
  void crearEscenario() {
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    docente =
        escenario.crearDocente("Julian Hoyos", escenario.crearPrograma("Software", ingenieria));
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    otroCoordinador =
        escenario.crearCoordinador("Paola Sarria", escenario.crearFacultad("Derecho"));
    periodo =
        escenario.abrirEtapa(
            escenario.crearPeriodoActivo("2026-2"),
            TipoEtapa.AGENDA_CITAS,
            BaseDatosPruebaConfig.HOY,
            BaseDatosPruebaConfig.HOY);
  }

  @Test
  void abreUnaFranjaDeTreintaMinutosYLaLista() throws Exception {
    abrir(coordinador, "09:00", "09:30")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.fecha").value(DIA.toString()))
        .andExpect(jsonPath("$.horaInicio").value("09:00:00"))
        .andExpect(jsonPath("$.horaFin").value("09:30:00"))
        .andExpect(jsonPath("$.estado").value("ABIERTA"))
        .andExpect(jsonPath("$.reservada").value(false));

    mockMvc.perform(get("/franjas").with(como(coordinador))).andExpect(jsonPath("$", hasSize(1)));
    mockMvc
        .perform(get("/franjas").with(como(otroCoordinador)))
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void elListadoSeFiltraPorFechaPorEstadoOPorAmbos() throws Exception {
    crear(coordinador, "08:00");
    crear(coordinador, "08:30").cerrar();
    franjaRepository.saveAndFlush(
        new FranjaHoraria(coordinador, periodo, DIA.plusDays(1), LocalTime.of(8, 0)));

    mockMvc
        .perform(get("/franjas").param("fecha", DIA.toString()).with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));
    mockMvc
        .perform(get("/franjas").param("estado", "CERRADA").with(como(coordinador)))
        .andExpect(jsonPath("$", hasSize(1)));
    mockMvc
        .perform(
            get("/franjas")
                .param("fecha", DIA.plusDays(1).toString())
                .param("estado", "ABIERTA")
                .with(como(coordinador)))
        .andExpect(jsonPath("$", hasSize(1)));
    mockMvc
        .perform(get("/franjas").param("fecha", "no-es-fecha").with(como(coordinador)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rechazaDuracionDistintaFechasPasadasYFueraDelPeriodo() throws Exception {
    abrir(coordinador, "09:00", "09:45")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje", containsString("30 minutos")));
    abrir(coordinador, "23:45", "00:15").andExpect(status().isBadRequest());
    abrir(coordinador, BaseDatosPruebaConfig.HOY.minusDays(1), "09:00", "09:30")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje", containsString("pasadas")));
    abrir(coordinador, LocalDate.of(2027, 3, 1), "09:00", "09:30")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje", containsString("periodo")));
  }

  @Test
  void noSePuedeAbrirUnaFranjaQueSeCruceConOtraAbiertaDelMismoCoordinador() throws Exception {
    abrir(coordinador, "09:00", "09:30").andExpect(status().isCreated());

    abrir(coordinador, "09:15", "09:45")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje", containsString("Se cruza")));
    // Pegada a la anterior sí se puede, y otro coordinador puede usar el mismo horario.
    abrir(coordinador, "09:30", "10:00").andExpect(status().isCreated());
    abrir(otroCoordinador, "09:00", "09:30").andExpect(status().isCreated());
  }

  @Test
  void laBaseDeDatosTambienImpideElSolapeAunqueNoSePaseSoloPorElServicio() {
    franjaRepository.saveAndFlush(
        new FranjaHoraria(coordinador, periodo, DIA, LocalTime.of(10, 0)));

    // Directo a la base. Debe ser lo último de la prueba: el error aborta la transacción.
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "insert into franjas_horarias (id_coordinador, id_periodo, fecha, hora_inicio,"
                        + " hora_fin) values (?, ?, ?, '10:15', '10:45')",
                    coordinador.getIdUsuario(),
                    periodo.getIdPeriodo(),
                    DIA))
        .hasMessageContaining("ex_franjas_sin_solape");
  }

  @Test
  void sobreUnaFranjaCerradaSePuedeAbrirOtraEnElMismoHorario() throws Exception {
    FranjaHoraria primera =
        franjaRepository.saveAndFlush(
            new FranjaHoraria(coordinador, periodo, DIA, LocalTime.of(10, 0)));

    mockMvc
        .perform(patch("/franjas/{id}/cierre", primera.getIdFranja()).with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("CERRADA"));
    abrir(coordinador, "10:00", "10:30").andExpect(status().isCreated());
  }

  @Test
  void elLoteAbreLoQuePuedeYExplicaLoQueOmite() throws Exception {
    abrir(coordinador, "14:00", "14:30");

    mockMvc
        .perform(
            post("/franjas/lote")
                .with(como(coordinador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        new LoteFranjasRequest(
                            AccionLote.ABRIR,
                            List.of(
                                franja(DIA, "08:00", "08:30"),
                                franja(DIA, "08:30", "09:00"),
                                franja(DIA, "08:15", "08:45"),
                                franja(DIA, "14:00", "14:30"),
                                franja(DIA, "15:00", "16:00")),
                            null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accion").value("ABRIR"))
        .andExpect(jsonPath("$.aplicadas", hasSize(2)))
        .andExpect(jsonPath("$.aplicadas[0].idFranja").isNumber())
        .andExpect(jsonPath("$.omitidas", hasSize(3)))
        .andExpect(jsonPath("$.omitidas[0].indice").value(2))
        .andExpect(jsonPath("$.omitidas[0].motivo", containsString("mismo lote")))
        .andExpect(jsonPath("$.omitidas[1].indice").value(3))
        .andExpect(jsonPath("$.omitidas[1].motivo", containsString("Se cruza")))
        .andExpect(jsonPath("$.omitidas[2].motivo", containsString("30 minutos")));
  }

  @Test
  void elLoteCierraSinTocarLasReservadasNiLasAjenas() throws Exception {
    FranjaHoraria libre = crear(coordinador, "08:00");
    FranjaHoraria reservada = crear(coordinador, "08:30");
    FranjaHoraria cerrada = crear(coordinador, "09:00");
    cerrada.cerrar();
    FranjaHoraria ajena = crear(otroCoordinador, "08:00");
    citaRepository.saveAndFlush(new Cita(reservada, docente));

    mockMvc
        .perform(
            post("/franjas/lote")
                .with(como(coordinador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        new LoteFranjasRequest(
                            AccionLote.CERRAR,
                            null,
                            Arrays.asList(
                                libre.getIdFranja(),
                                reservada.getIdFranja(),
                                cerrada.getIdFranja(),
                                ajena.getIdFranja())))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.aplicadas", hasSize(1)))
        .andExpect(jsonPath("$.aplicadas[0].idFranja").value(libre.getIdFranja()))
        .andExpect(jsonPath("$.omitidas[0].motivo").value("Tiene una cita reservada"))
        .andExpect(jsonPath("$.omitidas[1].motivo").value("La franja ya estaba cerrada"))
        .andExpect(jsonPath("$.omitidas[2].motivo").value("Franja no encontrada"));

    mockMvc
        .perform(get("/franjas").param("estado", "ABIERTA").with(como(coordinador)))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].reservada").value(true))
        .andExpect(jsonPath("$[0].reserva.nombreDocente").value("Julian Hoyos"));
  }

  @Test
  void unaFranjaReservadaNoSeCierraNiSeEliminaYUnaConHistorialNoSeElimina() throws Exception {
    FranjaHoraria reservada = crear(coordinador, "08:00");
    citaRepository.saveAndFlush(new Cita(reservada, docente));
    FranjaHoraria conHistorial = crear(coordinador, "09:00");
    Cita anterior =
        citaRepository.saveAndFlush(
            new Cita(
                conHistorial,
                escenario.crearDocente(
                    "Diana Astaiza",
                    escenario.crearPrograma("Otro", escenario.crearFacultad("Otra")))));
    anterior.liberar(MotivoLiberacion.REEMPLAZADA);
    citaRepository.saveAndFlush(anterior);
    FranjaHoraria libre = crear(coordinador, "10:00");

    mockMvc
        .perform(patch("/franjas/{id}/cierre", reservada.getIdFranja()).with(como(coordinador)))
        .andExpect(status().isConflict());
    mockMvc
        .perform(delete("/franjas/{id}", reservada.getIdFranja()).with(como(coordinador)))
        .andExpect(status().isConflict());
    mockMvc
        .perform(delete("/franjas/{id}", conHistorial.getIdFranja()).with(como(coordinador)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje", containsString("historial")));
    mockMvc
        .perform(delete("/franjas/{id}", libre.getIdFranja()).with(como(otroCoordinador)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(delete("/franjas/{id}", libre.getIdFranja()).with(como(coordinador)))
        .andExpect(status().isNoContent());
  }

  @Test
  void fueraDeLaAgendaDeCitasNoSeModificaPeroSeConsulta() throws Exception {
    escenario.abrirEtapa(
        periodo,
        TipoEtapa.AGENDA_CITAS,
        BaseDatosPruebaConfig.HOY.plusDays(1),
        BaseDatosPruebaConfig.HOY.plusDays(5));

    abrir(coordinador, "09:00", "09:30")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.etapa").value("AGENDA_CITAS"));
    mockMvc.perform(get("/franjas").with(como(coordinador))).andExpect(status().isOk());
  }

  @Test
  void soloElCoordinadorManejaFranjas() throws Exception {
    abrir(docente, "09:00", "09:30").andExpect(status().isForbidden());
    mockMvc.perform(get("/franjas").with(comoVicerrector())).andExpect(status().isForbidden());
  }

  private FranjaHoraria crear(Usuario dueno, String inicio) {
    return franjaRepository.saveAndFlush(
        new FranjaHoraria(dueno, periodo, DIA, LocalTime.parse(inicio)));
  }

  private FranjaRequest franja(LocalDate fecha, String inicio, String fin) {
    return new FranjaRequest(fecha, LocalTime.parse(inicio), LocalTime.parse(fin));
  }

  private ResultActions abrir(Usuario quien, String inicio, String fin) throws Exception {
    return abrir(quien, DIA, inicio, fin);
  }

  private ResultActions abrir(Usuario quien, LocalDate fecha, String inicio, String fin)
      throws Exception {
    return mockMvc.perform(
        post("/franjas")
            .with(como(quien))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(franja(fecha, inicio, fin))));
  }
}
