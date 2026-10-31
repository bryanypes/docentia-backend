package com.uniautonoma.docentia.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.ProgramaRequest;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.repository.FacultadRepository;
import com.uniautonoma.docentia.repository.ProgramaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class ProgramaControllerTest extends PruebaIntegracion {

  @Autowired private FacultadRepository facultadRepository;
  @Autowired private ProgramaRepository programaRepository;

  private Facultad ingenieria;
  private Facultad derecho;

  @BeforeEach
  void crearFacultades() {
    ingenieria = facultadRepository.save(new Facultad("Ingeniería"));
    derecho = facultadRepository.save(new Facultad("Derecho"));
  }

  @Test
  void creaUnProgramaEnSuFacultad() throws Exception {
    mockMvc
        .perform(
            post("/programas")
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        new ProgramaRequest("Ingeniería de Software", ingenieria.getIdFacultad()))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.idFacultad").value(ingenieria.getIdFacultad()))
        .andExpect(jsonPath("$.nombreFacultad").value("Ingeniería"));
  }

  @Test
  void noCreaUnProgramaEnUnaFacultadInexistente() throws Exception {
    mockMvc
        .perform(
            post("/programas")
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new ProgramaRequest("Ingeniería de Software", 99999L))))
        .andExpect(status().isNotFound());
  }

  @Test
  void exigeLaFacultad() throws Exception {
    mockMvc
        .perform(
            post("/programas")
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new ProgramaRequest("Ingeniería de Software", null))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.idFacultad").exists());
  }

  @Test
  void elMismoNombrePuedeRepetirseEnFacultadesDistintas() throws Exception {
    programaRepository.save(new Programa("Especialización en Gerencia", ingenieria));

    mockMvc
        .perform(
            post("/programas")
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        new ProgramaRequest(
                            "Especialización en Gerencia", derecho.getIdFacultad()))))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/programas")
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        new ProgramaRequest(
                            "especialización en gerencia", ingenieria.getIdFacultad()))))
        .andExpect(status().isConflict());
  }

  @Test
  void filtraLosProgramasPorFacultad() throws Exception {
    programaRepository.save(new Programa("Ingeniería de Software", ingenieria));
    programaRepository.save(new Programa("Ingeniería Civil", ingenieria));
    programaRepository.save(new Programa("Derecho", derecho));

    mockMvc
        .perform(
            get("/programas")
                .with(comoVicerrector())
                .param("idFacultad", ingenieria.getIdFacultad().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].nombre", contains("Ingeniería Civil", "Ingeniería de Software")));
  }

  @Test
  void trasladaUnProgramaDeFacultad() throws Exception {
    Programa programa = programaRepository.save(new Programa("Derecho Informático", ingenieria));

    mockMvc
        .perform(
            put("/programas/{id}", programa.getIdPrograma())
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(new ProgramaRequest("Derecho Informático", derecho.getIdFacultad()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombreFacultad").value("Derecho"));
  }

  @Test
  void alEliminarElProgramaDejaDeListarse() throws Exception {
    Programa programa = programaRepository.save(new Programa("Ingeniería de Software", ingenieria));

    mockMvc
        .perform(delete("/programas/{id}", programa.getIdPrograma()).with(comoVicerrector()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/programas/{id}", programa.getIdPrograma()).with(comoVicerrector()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(delete("/facultades/{id}", ingenieria.getIdFacultad()).with(comoVicerrector()))
        .andExpect(status().isNoContent());
  }
}
