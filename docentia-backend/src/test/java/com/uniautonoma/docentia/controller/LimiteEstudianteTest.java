package com.uniautonoma.docentia.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/** El formulario del estudiante tiene un tope de solicitudes por minuto. */
@TestPropertySource(properties = "docentia.heteroevaluacion.lecturas-por-minuto=3")
class LimiteEstudianteTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;

  @Test
  void superarElTopeResponde429() throws Exception {
    var programa = escenario.crearPrograma("Software", escenario.crearFacultad("Ingeniería"));
    Usuario estudiante = escenario.crearEstudiante("Estudiante Tope", programa);
    escenario.crearPeriodoActivo("2026-2");
    for (int i = 0; i < 3; i++) {
      mockMvc
          .perform(get("/estudiante/evaluaciones/{id}", 987654).with(como(estudiante)))
          .andExpect(status().isNotFound());
    }
    mockMvc
        .perform(get("/estudiante/evaluaciones/{id}", 987654).with(como(estudiante)))
        .andExpect(status().isTooManyRequests());
  }
}
