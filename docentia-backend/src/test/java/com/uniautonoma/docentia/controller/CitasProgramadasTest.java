package com.uniautonoma.docentia.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CitaRepository;
import com.uniautonoma.docentia.repository.FranjaHorariaRepository;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** La administración ve todas las citas del periodo: coordinación con docentes y vicerrectoría. */
class CitasProgramadasTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CitaRepository citaRepository;
  @Autowired private FranjaHorariaRepository franjaRepository;

  private Usuario coordinador;
  private Usuario docente;
  private Usuario vicerrector;
  private Usuario administrador;
  private PeriodoAcademico periodo;

  @BeforeEach
  void crearEscenario() {
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    docente = escenario.crearDocente("Nora Munoz", escenario.crearPrograma("Software", ingenieria));
    vicerrector = escenario.crearVicerrector("Elena Munoz");
    administrador = escenario.crearConRol("Ana Gomez", Rol.ADMINISTRADOR);
    periodo = escenario.crearPeriodoActivo("2026-2");
  }

  private FranjaHoraria franja(Usuario duenio, int dias, int hora) {
    return franjaRepository.saveAndFlush(
        new FranjaHoraria(
            duenio, periodo, BaseDatosPruebaConfig.HOY.plusDays(dias), LocalTime.of(hora, 0)));
  }

  @Test
  void laAdministracionVeLasCitasDeCoordinacionYDeVicerrectoriaPorFecha() throws Exception {
    citaRepository.saveAndFlush(new Cita(franja(vicerrector, 2, 10), coordinador));
    citaRepository.saveAndFlush(new Cita(franja(coordinador, 1, 9), docente));

    mockMvc
        .perform(get("/citas/programadas").with(como(administrador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].nombreDocente").value("Nora Munoz"))
        .andExpect(jsonPath("$[0].unidad").value("Software"))
        .andExpect(jsonPath("$[0].nombreAnfitrion").value("Marta Ruiz"))
        .andExpect(jsonPath("$[0].rolAnfitrion").value("COORDINADOR"))
        .andExpect(jsonPath("$[1].nombreDocente").value("Marta Ruiz"))
        .andExpect(jsonPath("$[1].rolDocente").value("COORDINADOR"))
        .andExpect(jsonPath("$[1].rolAnfitrion").value("VICERRECTOR"))
        .andExpect(jsonPath("$[0].promedio").doesNotExist());
  }

  @Test
  void soloLaAdministracion() throws Exception {
    mockMvc
        .perform(get("/citas/programadas").with(como(coordinador)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/citas/programadas").with(como(vicerrector)))
        .andExpect(status().isForbidden());
  }
}
