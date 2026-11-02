package com.uniautonoma.docentia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.service.importacion.ImportacionService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** La carga de notas por CSV quedó cortada: la fuente ESTUDIANTES sale del formulario HETERO. */
class ImportacionCsvTest extends PruebaIntegracion {

  private static final String ENCABEZADO =
      "Correo docente;Dominio disciplinar;Planeación académica;Metodología de enseñanza;"
          + "Evaluación del aprendizaje;Compromiso institucional;Comentario\n";

  @Autowired private EscenarioPrueba escenario;

  private Usuario julian;
  private Usuario coordinador;

  @BeforeEach
  void crearEscenario() {
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    julian =
        escenario.crearDocente(
            "Julian Hoyos", escenario.crearPrograma("Ingeniería de Software", ingenieria));
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    escenario.crearPeriodoActivo("2026-2");
  }

  @Test
  void validarEImportarRespondenQueLaNotaSaleDelFormulario() throws Exception {
    enviar("/importaciones/csv/validar", csvValido(), como(coordinador))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value(ImportacionService.MENSAJE_CSV_CORTADO));
    enviar("/importaciones/csv", csvValido(), como(coordinador))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.mensaje").value(ImportacionService.MENSAJE_CSV_CORTADO));
  }

  @Test
  void soloElCoordinadorIntentaCargarYElHistorialSigueVisible() throws Exception {
    enviar("/importaciones/csv", csvValido(), como(julian)).andExpect(status().isForbidden());
    enviar("/importaciones/csv", csvValido(), comoVicerrector()).andExpect(status().isForbidden());
    mockMvc
        .perform(multipart("/importaciones/csv").file(csvValido()))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/importaciones").with(como(coordinador)))
        .andExpect(jsonPath("$", hasSize(0)));
    mockMvc.perform(get("/importaciones").with(como(julian))).andExpect(status().isForbidden());
  }

  @Test
  void unArchivoDemasiadoGrandeOSinArchivoRespondenSinTraza() throws Exception {
    byte[] grande = new byte[2 * 1024 * 1024 + 1];

    enviar(
            "/importaciones/csv",
            new MockMultipartFile("archivo", "grande.csv", "text/csv", grande),
            como(coordinador))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.mensaje", containsString("pesa más de")));
    mockMvc
        .perform(multipart("/importaciones/csv").with(como(coordinador)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("Falta la parte \"archivo\" del formulario"))
        .andExpect(jsonPath("$.trace").doesNotExist());
  }

  private MockMultipartFile csvValido() {
    return new MockMultipartFile(
        "archivo",
        "evaluacion.csv",
        "text/csv",
        (ENCABEZADO + "julian.hoyos@uniautonoma.edu.co;4.5;4;4;4;4;Explica muy bien\n")
            .getBytes(StandardCharsets.UTF_8));
  }

  private ResultActions enviar(String ruta, MockMultipartFile archivo, RequestPostProcessor quien)
      throws Exception {
    return mockMvc.perform(multipart(ruta).file(archivo).with(quien));
  }
}
