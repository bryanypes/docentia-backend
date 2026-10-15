package com.uniautonoma.docentia.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.InicioSesionRequest;
import com.uniautonoma.docentia.dto.UsuarioRequest;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class UsuarioControllerTest extends PruebaIntegracion {

  private static final String CONTRASENA = "Clave-de-prueba-1";

  @Autowired private EscenarioPrueba escenario;

  private Facultad ingenieria;
  private Facultad derecho;
  private Programa software;

  @BeforeEach
  void crearFacultadesYProgramas() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    derecho = escenario.crearFacultad("Derecho");
    software = escenario.crearPrograma("Ingeniería de Software", ingenieria);
    escenario.crearDocente("Luisa Mosquera", escenario.crearPrograma("Derecho", derecho));
  }

  @Test
  void elVicerrectorCreaUnUsuarioDeCadaRolConSuVinculo() throws Exception {
    crear(solicitud("coord@prueba.co", Rol.COORDINADOR, ingenieria.getIdFacultad(), null))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.rol").value("COORDINADOR"))
        .andExpect(jsonPath("$.nombreFacultad").value("Ingeniería"))
        .andExpect(jsonPath("$.idPrograma").doesNotExist())
        .andExpect(jsonPath("$.contrasenaHash").doesNotExist());
    crear(solicitud("docente@prueba.co", Rol.DOCENTE, null, software.getIdPrograma()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nombrePrograma").value("Ingeniería de Software"))
        .andExpect(jsonPath("$.idFacultad").value(ingenieria.getIdFacultad()));
    crear(solicitud("vice@prueba.co", Rol.VICERRECTOR, null, null))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.idFacultad").doesNotExist());
  }

  @Test
  void rechazaUnVinculoQueNoCorrespondeAlRol() throws Exception {
    crear(solicitud("a@prueba.co", Rol.COORDINADOR, null, null))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.mensaje").value("El coordinador debe estar vinculado a una facultad"));
    crear(solicitud("b@prueba.co", Rol.DOCENTE, ingenieria.getIdFacultad(), null))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("El docente debe estar vinculado a un programa"));
    crear(solicitud("c@prueba.co", Rol.VICERRECTOR, ingenieria.getIdFacultad(), null))
        .andExpect(status().isBadRequest());
    crear(solicitud("d@prueba.co", Rol.DOCENTE, derecho.getIdFacultad(), software.getIdPrograma()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("El programa no pertenece a la facultad indicada"));
  }

  @Test
  void rechazaRolFaltanteOInexistente() throws Exception {
    crear(solicitud("e@prueba.co", null, null, null))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores.rol").value("El rol es obligatorio"));
    mockMvc
        .perform(
            post("/usuarios")
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nombreCompleto":"X","correo":"f@prueba.co","rol":["DOCENTE","COORDINADOR"],
                     "contrasena":"Clave-de-prueba-1"}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void unaFacultadOProgramaInexistenteRespondeNoEncontrado() throws Exception {
    crear(solicitud("g@prueba.co", Rol.COORDINADOR, 999_999L, null))
        .andExpect(status().isNotFound());
    crear(solicitud("h@prueba.co", Rol.DOCENTE, null, 999_999L)).andExpect(status().isNotFound());
  }

  @Test
  void elCorreoNoSePuedeRepetirAunqueCambienLasMayusculas() throws Exception {
    crear(solicitud("repetido@prueba.co", Rol.VICERRECTOR, null, null))
        .andExpect(status().isCreated());
    crear(solicitud("Repetido@Prueba.co", Rol.VICERRECTOR, null, null))
        .andExpect(status().isConflict());
  }

  @Test
  void crearExigeContrasena() throws Exception {
    UsuarioRequest sinContrasena =
        new UsuarioRequest("Ana Ruiz", "i@prueba.co", Rol.VICERRECTOR, null, null, null);
    crear(sinContrasena)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.mensaje").value("La contraseña es obligatoria al crear un usuario"));
  }

  @Test
  void editarCambiaElRolYElVinculoSinTocarLaContrasena() throws Exception {
    Usuario docente = escenario.crearDocente("Julian Hoyos", software);

    mockMvc
        .perform(
            put("/usuarios/{id}", docente.getIdUsuario())
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        new UsuarioRequest(
                            "Julian Hoyos",
                            docente.getCorreo(),
                            Rol.COORDINADOR,
                            derecho.getIdFacultad(),
                            null,
                            null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rol").value("COORDINADOR"))
        .andExpect(jsonPath("$.nombreFacultad").value("Derecho"))
        .andExpect(jsonPath("$.idPrograma").doesNotExist());

    mockMvc
        .perform(get("/usuarios").param("rol", "COORDINADOR").with(comoVicerrector()))
        .andExpect(jsonPath("$[*].nombreCompleto", contains("Julian Hoyos")));
  }

  @Test
  void editarUnUsuarioInexistenteRespondeNoEncontrado() throws Exception {
    mockMvc
        .perform(
            put("/usuarios/{id}", 999_999)
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(solicitud("x@prueba.co", Rol.VICERRECTOR, null, null))))
        .andExpect(status().isNotFound());
  }

  @Test
  void soloElVicerrectorAdministraUsuarios() throws Exception {
    Usuario docente = escenario.crearDocente("Julian Hoyos", software);
    RequestPostProcessor coordinador = comoCoordinador(ingenieria.getIdFacultad());

    mockMvc.perform(get("/usuarios").with(coordinador)).andExpect(status().isForbidden());
    mockMvc.perform(get("/usuarios").with(como(docente))).andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/usuarios")
                .with(coordinador)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(solicitud("y@prueba.co", Rol.DOCENTE, null, software.getIdPrograma()))))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/usuarios")).andExpect(status().isUnauthorized());
  }

  @Test
  void elUsuarioCreadoIniciaSesionYSuAlcanceViajaEnElToken() throws Exception {
    crear(solicitud("coord@prueba.co", Rol.COORDINADOR, ingenieria.getIdFacultad(), null))
        .andExpect(status().isCreated());
    escenario.crearDocente("Julian Hoyos", software);

    String cuerpo =
        mockMvc
            .perform(
                post("/sesiones")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(aJson(new InicioSesionRequest("coord@prueba.co", CONTRASENA))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.usuario.rol").value("COORDINADOR"))
            .andExpect(jsonPath("$.usuario.idFacultad").value(ingenieria.getIdFacultad()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode sesion = objectMapper.readTree(cuerpo);

    // Con el token real (sin simular la sesión) el filtro por facultad sale de los claims.
    mockMvc
        .perform(
            get("/docentes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sesion.get("token").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].nombreCompleto", contains("Julian Hoyos")));
  }

  private ResultActions crear(UsuarioRequest solicitud) throws Exception {
    return mockMvc.perform(
        post("/usuarios")
            .with(comoVicerrector())
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(solicitud)));
  }

  private UsuarioRequest solicitud(String correo, Rol rol, Long idFacultad, Long idPrograma) {
    return new UsuarioRequest("Ana Ruiz", correo, rol, idFacultad, idPrograma, CONTRASENA);
  }
}
