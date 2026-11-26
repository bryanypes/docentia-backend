package com.uniautonoma.docentia.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** El tutorial guiado se muestra hasta que el usuario lo termina o lo salta. */
class TutorialTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;

  private Usuario coordinador;
  private Usuario otro;

  @BeforeEach
  void crearEscenario() {
    coordinador = escenario.crearCoordinador("Marta Ruiz", escenario.crearFacultad("Ingeniería"));
    otro = escenario.crearVicerrector("Elena Ficcion");
  }

  @Test
  void nadieLoHaVistoAlEmpezarYQuedaVistoSoloParaQuienLoTermina() throws Exception {
    mockMvc
        .perform(get("/sesiones/actual/tutorial").with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.visto").value(false));

    mockMvc
        .perform(put("/sesiones/actual/tutorial").with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.visto").value(true));
    mockMvc
        .perform(get("/sesiones/actual/tutorial").with(como(coordinador)))
        .andExpect(jsonPath("$.visto").value(true));
    mockMvc
        .perform(get("/sesiones/actual/tutorial").with(como(otro)))
        .andExpect(jsonPath("$.visto").value(false));
  }

  @Test
  void sePuedeVolverAVerDesdeElPerfil() throws Exception {
    mockMvc.perform(put("/sesiones/actual/tutorial").with(como(coordinador)));

    mockMvc
        .perform(delete("/sesiones/actual/tutorial").with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.visto").value(false));
    mockMvc
        .perform(get("/sesiones/actual/tutorial").with(como(coordinador)))
        .andExpect(jsonPath("$.visto").value(false));
  }
}
