package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.FacultadRequest;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.repository.FacultadRepository;
import com.uniautonoma.docentia.repository.ProgramaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class FacultadControllerTest extends PruebaIntegracion {

  @Autowired private FacultadRepository facultadRepository;
  @Autowired private ProgramaRepository programaRepository;

  @Test
  void creaUnaFacultad() throws Exception {
    mockMvc
        .perform(
            post("/facultades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new FacultadRequest("  Ingeniería  "))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.idFacultad").isNumber())
        .andExpect(jsonPath("$.nombre").value("Ingeniería"));
  }

  @Test
  void rechazaUnNombreVacio() throws Exception {
    mockMvc
        .perform(
            post("/facultades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new FacultadRequest(" "))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.nombre").value("El nombre es obligatorio"));
  }

  @Test
  void rechazaUnNombreRepetido() throws Exception {
    facultadRepository.save(new Facultad("Ingeniería"));

    mockMvc
        .perform(
            post("/facultades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new FacultadRequest("ingeniería"))))
        .andExpect(status().isConflict());
  }

  @Test
  void listaSoloLasFacultadesActivasEnOrdenAlfabetico() throws Exception {
    facultadRepository.save(new Facultad("Ingeniería"));
    facultadRepository.save(new Facultad("Derecho"));
    Facultad eliminada = new Facultad("Artes");
    eliminada.setEsActiva(false);
    facultadRepository.save(eliminada);

    mockMvc
        .perform(get("/facultades"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].nombre", contains("Derecho", "Ingeniería")));
  }

  @Test
  void actualizaElNombre() throws Exception {
    Facultad facultad = facultadRepository.save(new Facultad("Ingenieria"));

    mockMvc
        .perform(
            put("/facultades/{id}", facultad.getIdFacultad())
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new FacultadRequest("Ingeniería"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Ingeniería"));
  }

  @Test
  void noEliminaUnaFacultadConProgramasActivos() throws Exception {
    Facultad facultad = facultadRepository.save(new Facultad("Ingeniería"));
    programaRepository.save(new Programa("Ingeniería de Software", facultad));

    mockMvc
        .perform(delete("/facultades/{id}", facultad.getIdFacultad()))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.mensaje")
                .value("No se puede eliminar la facultad porque tiene programas activos"));

    assertThat(facultadRepository.findByIdFacultadAndEsActivaTrue(facultad.getIdFacultad()))
        .isPresent();
  }

  @Test
  void eliminaUnaFacultadCuyosProgramasYaEstanInactivos() throws Exception {
    Facultad facultad = facultadRepository.save(new Facultad("Ingeniería"));
    Programa programa = new Programa("Ingeniería de Software", facultad);
    programa.setEsActivo(false);
    programaRepository.save(programa);

    mockMvc
        .perform(delete("/facultades/{id}", facultad.getIdFacultad()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/facultades/{id}", facultad.getIdFacultad()))
        .andExpect(status().isNotFound());
  }

  @Test
  void respondeNoEncontradoSiLaFacultadNoExiste() throws Exception {
    mockMvc.perform(get("/facultades/{id}", 99999)).andExpect(status().isNotFound());
  }
}
