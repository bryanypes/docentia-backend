package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.PlantillaMapeoRequest;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.service.importacion.CampoImportacion;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * C5: el usuario asigna las columnas que el sistema no reconoce y guarda el mapeo como plantilla.
 */
class MapeoColumnasTest extends PruebaIntegracion {

  private static final List<String> COLUMNAS =
      List.of("Email", "Dom.", "Plan.", "Metod.", "Eval.", "Comp.", "Obs");
  private static final String CSV_POWER_BI =
      String.join(";", COLUMNAS) + "\njulian.hoyos@uniautonoma.edu.co;4;4;4;4;4;Bien\n";

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CalificacionRepository calificacionRepository;

  private Usuario coordinador;

  @BeforeEach
  void crearEscenario() {
    Facultad ingenieria = escenario.crearFacultad("Ingeniería");
    escenario.crearDocente(
        "Julian Hoyos", escenario.crearPrograma("Ingeniería de Software", ingenieria));
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    escenario.crearPeriodoActivo("2026-2");
  }

  @Test
  void conColumnasQueNoSeReconocenValidarDevuelveLasDetectadasYLoQueFalta() throws Exception {
    enviar("/importaciones/csv/validar", CSV_POWER_BI)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valido").value(false))
        .andExpect(jsonPath("$.origenMapeo").value("AUTOMATICO"))
        .andExpect(jsonPath("$.columnas", contains(COLUMNAS.toArray())))
        .andExpect(jsonPath("$.mapeo.Email").value("CORREO_DOCENTE"))
        .andExpect(jsonPath("$.camposObligatoriosSinMapear", hasSize(5)));
  }

  @Test
  void conElMapeoDelUsuarioSeValidaYSeImportaEnUnSoloFlujo() throws Exception {
    String mapeo = aJson(mapeoPowerBi());

    enviar("/importaciones/csv/validar", CSV_POWER_BI, "mapeo", mapeo)
        .andExpect(jsonPath("$.valido").value(true))
        .andExpect(jsonPath("$.origenMapeo").value("MANUAL"))
        .andExpect(jsonPath("$.mapeo['Dom.']").value("DOMINIO_DISCIPLINAR"));
    enviar("/importaciones/csv", CSV_POWER_BI, "mapeo", mapeo)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.importacion.notasNuevas").value(1));
  }

  @Test
  void siElMapeoDejaUnCampoObligatorioSinAsignarLaImportacionSeRechaza() throws Exception {
    Map<String, CampoImportacion> incompleto = new LinkedHashMap<>(mapeoPowerBi());
    incompleto.remove("Comp.");

    enviar("/importaciones/csv", CSV_POWER_BI, "mapeo", aJson(incompleto))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.reporte.camposObligatoriosSinMapear", contains("COMPROMISO_INSTITUCIONAL")))
        .andExpect(
            jsonPath("$.reporte.errores[0].motivo", containsString("Compromiso institucional")));
    assertThat(calificacionRepository.count()).isZero();
  }

  @Test
  void unMapeoMalFormadoOConUnCampoDesconocidoEsUnaPeticionInvalida() throws Exception {
    enviar("/importaciones/csv/validar", CSV_POWER_BI, "mapeo", "{no json")
        .andExpect(status().isBadRequest());
    enviar("/importaciones/csv/validar", CSV_POWER_BI, "mapeo", "{\"Email\":\"TELEFONO\"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje", containsString("columna a campo")));
  }

  @Test
  void laPlantillaGuardadaSeReutilizaSolaSiLasColumnasCoinciden() throws Exception {
    guardarPlantilla("Power BI", COLUMNAS, mapeoPowerBi())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Power BI"))
        .andExpect(jsonPath("$.mapeo['Email']").value("CORREO_DOCENTE"));

    enviar("/importaciones/csv", CSV_POWER_BI)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reporte.origenMapeo").value("PLANTILLA"))
        .andExpect(jsonPath("$.reporte.plantilla").value("Power BI"));
  }

  @Test
  void conOtrasColumnasLaPlantillaAvisaQueCambioYLaImportacionSeRechaza() throws Exception {
    guardarPlantilla("Power BI", COLUMNAS, mapeoPowerBi());
    String csvCambiado =
        "Email;Dom.;Plan.;Metodología;Eval.;Comp.;Obs\n"
            + "julian.hoyos@uniautonoma.edu.co;4;4;4;4;4;Bien\n";

    enviar("/importaciones/csv/validar", csvCambiado, "plantilla", "power bi")
        .andExpect(jsonPath("$.valido").value(false))
        .andExpect(jsonPath("$.plantilla").value("Power BI"))
        .andExpect(jsonPath("$.columnasFaltantes", contains("Metod.")))
        .andExpect(jsonPath("$.columnasNuevas", contains("Metodología")))
        .andExpect(jsonPath("$.camposObligatoriosSinMapear", contains("METODOLOGIA_ENSENANZA")))
        .andExpect(jsonPath("$.advertencias[0]", containsString("no tiene las mismas columnas")));
    enviar("/importaciones/csv", csvCambiado, "plantilla", "Power BI")
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void guardarConElMismoNombreReemplazaYUnaPlantillaIncompletaNoSeGuarda() throws Exception {
    guardarPlantilla("Power BI", COLUMNAS, mapeoPowerBi());
    guardarPlantilla("Power BI", COLUMNAS, mapeoPowerBi());
    Map<String, CampoImportacion> sinCorreo = new LinkedHashMap<>(mapeoPowerBi());
    sinCorreo.remove("Email");

    guardarPlantilla("Incompleta", COLUMNAS, sinCorreo)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje", containsString("Correo del docente")));
    mockMvc
        .perform(get("/importaciones/plantillas").with(como(coordinador)))
        .andExpect(jsonPath("$[*].nombre", contains("Power BI")));
  }

  @Test
  void cadaCoordinadorSoloVeYUsaSusPlantillas() throws Exception {
    String cuerpo =
        guardarPlantilla("Power BI", COLUMNAS, mapeoPowerBi())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long idPlantilla = objectMapper.readTree(cuerpo).get("idPlantilla").asLong();
    Usuario otro = escenario.crearCoordinador("Pedro Paz", escenario.crearFacultad("Derecho"));

    mockMvc
        .perform(get("/importaciones/plantillas").with(como(otro)))
        .andExpect(jsonPath("$", hasSize(0)));
    mockMvc
        .perform(
            multipart("/importaciones/csv/validar")
                .file(archivo(CSV_POWER_BI))
                .param("plantilla", "Power BI")
                .with(como(otro)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("Plantilla no encontrada"));
    mockMvc
        .perform(delete("/importaciones/plantillas/{id}", idPlantilla).with(como(otro)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(delete("/importaciones/plantillas/{id}", idPlantilla).with(como(coordinador)))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(get("/importaciones/plantillas").with(comoVicerrector()))
        .andExpect(status().isForbidden());
  }

  private ResultActions guardarPlantilla(
      String nombre, List<String> columnas, Map<String, CampoImportacion> mapeo) throws Exception {
    return mockMvc.perform(
        post("/importaciones/plantillas")
            .with(como(coordinador))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new PlantillaMapeoRequest(nombre, columnas, mapeo))));
  }

  /** Envía el CSV como coordinador, con pares nombre/valor de campos adicionales del formulario. */
  private ResultActions enviar(String ruta, String csv, String... campos) throws Exception {
    MockMultipartHttpServletRequestBuilder peticion = multipart(ruta).file(archivo(csv));
    for (int i = 0; i < campos.length; i += 2) {
      peticion.param(campos[i], campos[i + 1]);
    }
    return mockMvc.perform(peticion.with(como(coordinador)));
  }

  private MockMultipartFile archivo(String csv) {
    return new MockMultipartFile(
        "archivo", "power-bi.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
  }

  private Map<String, CampoImportacion> mapeoPowerBi() {
    Map<String, CampoImportacion> mapeo = new LinkedHashMap<>();
    mapeo.put("Email", CampoImportacion.CORREO_DOCENTE);
    mapeo.put("Dom.", CampoImportacion.DOMINIO_DISCIPLINAR);
    mapeo.put("Plan.", CampoImportacion.PLANEACION_ACADEMICA);
    mapeo.put("Metod.", CampoImportacion.METODOLOGIA_ENSENANZA);
    mapeo.put("Eval.", CampoImportacion.EVALUACION_APRENDIZAJE);
    mapeo.put("Comp.", CampoImportacion.COMPROMISO_INSTITUCIONAL);
    mapeo.put("Obs", CampoImportacion.COMENTARIO);
    return mapeo;
  }
}
