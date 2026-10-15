package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.EstadoCita;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import com.uniautonoma.docentia.entity.MotivoLiberacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CitaRepository;
import com.uniautonoma.docentia.repository.FranjaHorariaRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

/** F2: el docente reserva una franja de su coordinador; una sola cita activa por periodo. */
class ReservaFranjaTest extends PruebaIntegracion {

  private static final LocalDate DIA = BaseDatosPruebaConfig.HOY.plusDays(7);

  @Autowired private EscenarioPrueba escenario;
  @Autowired private FranjaHorariaRepository franjaRepository;
  @Autowired private CitaRepository citaRepository;
  @Autowired private EntityManager entityManager;

  private PeriodoAcademico periodo;
  private Usuario julian;
  private Usuario diana;
  private Usuario luisa;
  private FranjaHoraria ocho;
  private FranjaHoraria ochoYMedia;
  private FranjaHoraria deDerecho;

  @BeforeEach
  void crearEscenario() {
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    Facultad derecho = escenario.crearFacultad("Derecho");
    var software = escenario.crearPrograma("Software", ingenieria);
    julian = escenario.crearDocente("Julian Hoyos", software);
    diana = escenario.crearDocente("Diana Astaiza", software);
    luisa = escenario.crearDocente("Luisa Mosquera", escenario.crearPrograma("Derecho", derecho));
    Usuario coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    Usuario coordinadoraDerecho = escenario.crearCoordinador("Paola Sarria", derecho);
    periodo =
        escenario.abrirEtapa(
            escenario.crearPeriodoActivo("2026-2"),
            TipoEtapa.AGENDA_CITAS,
            BaseDatosPruebaConfig.HOY,
            BaseDatosPruebaConfig.HOY);
    ocho = franja(coordinador, DIA, "08:00");
    ochoYMedia = franja(coordinador, DIA, "08:30");
    FranjaHoraria cerrada = franja(coordinador, DIA, "09:00");
    cerrada.cerrar();
    franja(coordinador, BaseDatosPruebaConfig.HOY.minusDays(1), "08:00");
    deDerecho = franja(coordinadoraDerecho, DIA, "08:00");
  }

  @Test
  void elDocenteSoloVeLasFranjasLibresYVigentesDeSuCoordinador() throws Exception {
    mockMvc
        .perform(get("/franjas/disponibles").with(como(julian)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].idFranja", contains(id(ocho), id(ochoYMedia))))
        .andExpect(jsonPath("$[0].nombreCoordinador").value("Marta Ruiz"));
    mockMvc
        .perform(get("/franjas/disponibles").with(como(luisa)))
        .andExpect(jsonPath("$[*].idFranja", contains(id(deDerecho))));
  }

  @Test
  void alReservarLaFranjaDejaDeEstarDisponibleParaLosDemas() throws Exception {
    reservar(julian, ocho)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("ACTIVA"))
        .andExpect(jsonPath("$.idFranja").value(ocho.getIdFranja()))
        .andExpect(jsonPath("$.horaInicio").value("08:00:00"))
        .andExpect(jsonPath("$.idCitaLiberada").doesNotExist());

    mockMvc
        .perform(get("/franjas/disponibles").with(como(diana)))
        .andExpect(jsonPath("$[*].idFranja", contains(id(ochoYMedia))));
    reservar(diana, ocho)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("La franja ya fue reservada por otro docente"));
    mockMvc
        .perform(get("/citas/mia").with(como(julian)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idFranja").value(ocho.getIdFranja()));
    mockMvc.perform(get("/citas/mia").with(como(diana))).andExpect(status().isNoContent());
  }

  @Test
  void reservarUnaSegundaFranjaLiberaLaPrimeraEnLaMismaOperacion() throws Exception {
    long primera = idCita(reservar(julian, ocho));

    reservar(julian, ochoYMedia)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.idFranja").value(ochoYMedia.getIdFranja()))
        .andExpect(jsonPath("$.idCitaLiberada").value(primera));

    entityManager.clear();
    Cita anterior = citaRepository.findById(primera).orElseThrow();
    assertThat(anterior.getEstado()).isEqualTo(EstadoCita.LIBERADA);
    assertThat(anterior.getMotivoLiberacion()).isEqualTo(MotivoLiberacion.REEMPLAZADA);
    assertThat(anterior.getFechaLiberacion()).isNotNull();
    // La primera franja vuelve a estar libre para los demás.
    mockMvc
        .perform(get("/franjas/disponibles").with(como(diana)))
        .andExpect(jsonPath("$[*].idFranja", contains(id(ocho))));
    mockMvc
        .perform(get("/citas/mia").with(como(julian)))
        .andExpect(jsonPath("$.idFranja").value(ochoYMedia.getIdFranja()));
  }

  @Test
  void siLaNuevaReservaFallaElDocenteConservaLaQueTenia() throws Exception {
    long suya = idCita(reservar(julian, ocho));
    reservar(diana, ochoYMedia).andExpect(status().isCreated());

    reservar(julian, ochoYMedia).andExpect(status().isConflict());
    reservar(julian, ocho)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("Ya tienes reservada esta franja"));

    entityManager.clear();
    assertThat(citaRepository.findById(suya).orElseThrow().getEstado())
        .isEqualTo(EstadoCita.ACTIVA);
  }

  @Test
  void noSeReservanFranjasCerradasPasadasNiDeOtraFacultad() throws Exception {
    List<FranjaHoraria> todas =
        franjaRepository.buscarDelCoordinador(
            ocho.getCoordinador().getIdUsuario(), periodo.getIdPeriodo(), null, null);
    FranjaHoraria pasada = todas.get(0);
    FranjaHoraria cerrada = todas.get(todas.size() - 1);

    reservar(julian, cerrada)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("La franja está cerrada"));
    reservar(julian, pasada)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value("La franja ya pasó"));
    reservar(julian, deDerecho)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("Franja no encontrada"));
  }

  @Test
  void fueraDeLaAgendaDeCitasNoSeReserva() throws Exception {
    escenario.abrirEtapa(
        periodo,
        TipoEtapa.AGENDA_CITAS,
        BaseDatosPruebaConfig.HOY.plusDays(1),
        BaseDatosPruebaConfig.HOY.plusDays(3));

    reservar(julian, ocho)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.etapa").value("AGENDA_CITAS"));
  }

  @Test
  void soloElDocenteReserva() throws Exception {
    mockMvc
        .perform(post("/franjas/{id}/reserva", ocho.getIdFranja()).with(comoVicerrector()))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/franjas/disponibles").with(comoCoordinador(1L)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(post("/franjas/{id}/reserva", 999_999).with(como(julian)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje", containsString("Franja")));
  }

  private FranjaHoraria franja(Usuario coordinador, LocalDate fecha, String inicio) {
    return franjaRepository.saveAndFlush(
        new FranjaHoraria(coordinador, periodo, fecha, LocalTime.parse(inicio)));
  }

  private ResultActions reservar(Usuario docente, FranjaHoraria franja) throws Exception {
    return mockMvc.perform(post("/franjas/{id}/reserva", franja.getIdFranja()).with(como(docente)));
  }

  private long idCita(ResultActions resultado) throws Exception {
    return objectMapper
        .readTree(resultado.andReturn().getResponse().getContentAsString())
        .get("idCita")
        .asLong();
  }

  private int id(FranjaHoraria franja) {
    return franja.getIdFranja().intValue();
  }
}
