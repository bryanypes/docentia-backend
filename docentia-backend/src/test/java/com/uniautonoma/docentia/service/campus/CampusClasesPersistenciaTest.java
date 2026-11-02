package com.uniautonoma.docentia.service.campus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.InicioSesionRequest;
import com.uniautonoma.docentia.entity.Clase;
import com.uniautonoma.docentia.entity.Matricula;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.ClaseRepository;
import com.uniautonoma.docentia.repository.MatriculaRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

/** HU-01 etapa 1: el JSON de Campus se vuelve clases y matrículas, sin formulario todavía. */
class CampusClasesPersistenciaTest extends PruebaIntegracion {

  private static final String KAREN = "karen.mancilla@uniautonoma.edu.co";
  private static final String ANDREA = "andrea.nieto@uniautonoma.edu.co";
  private static final String ZULEMA = "zulema.leon@uniautonoma.edu.co";

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CargadorClasesCampus cargador;
  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private MatriculaRepository matriculaRepository;
  @Autowired private ClaseRepository claseRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void elSeedDejaAKarenConTresClasesYAndreaSoloEnElGrupoB() {
    PeriodoAcademico periodo = escenario.crearPeriodoActivo("2026-2");
    cargador.cargarDemo(periodo, "hash-de-prueba");

    Usuario karen = usuario(KAREN);
    assertThat(karen.getRol()).isEqualTo(Rol.ESTUDIANTE);
    assertThat(karen.getPrograma().getCodigo()).isEqualTo("CU027");

    List<Matricula> deKaren =
        matriculaRepository.findByEstudianteIdUsuarioAndEsActivaTrue(karen.getIdUsuario());
    assertThat(deKaren).hasSize(3);
    assertThat(deKaren)
        .extracting(matricula -> matricula.getClase().getCurso().getCodigo())
        .containsExactlyInAnyOrder("19072209", "12190626", "12190835");
    assertThat(
            deKaren.stream()
                .filter(m -> m.getClase().getCurso().getCodigo().equals("12190626"))
                .map(m -> m.getClase().getGrupo())
                .toList())
        .containsExactly("A");

    Usuario andrea = usuario(ANDREA);
    List<Matricula> deAndrea =
        matriculaRepository.findByEstudianteIdUsuarioAndEsActivaTrue(andrea.getIdUsuario());
    assertThat(deAndrea).hasSize(1);
    assertThat(deAndrea.getFirst().getClase().getGrupo()).isEqualTo("B");
    assertThat(deAndrea.getFirst().getClase().getCurso().getCodigo()).isEqualTo("12190626");
  }

  @Test
  void elMismoCursoEnGrupoAYgrupoBSonClasesDistintasDeZulema() {
    PeriodoAcademico periodo = escenario.crearPeriodoActivo("2026-2");
    cargador.cargarDemo(periodo, "hash-de-prueba");

    Usuario zulema = usuario(ZULEMA);
    assertThat(zulema.getRol()).isEqualTo(Rol.DOCENTE);
    List<Clase> clases =
        claseRepository.findByPeriodoIdPeriodoAndDocentesIdUsuario(
            periodo.getIdPeriodo(), zulema.getIdUsuario());
    assertThat(clases).hasSize(2);
    assertThat(clases).extracting(Clase::getGrupo).containsExactlyInAnyOrder("A", "B");
    assertThat(clases.get(0).getId()).isNotEqualTo(clases.get(1).getId());
    assertThat(clases).allMatch(clase -> "12190626".equals(clase.getCurso().getCodigo()));
  }

  @Test
  void karenEntraAunqueLaBaseYaTengaElSeedAnterior() throws Exception {
    escenario.crearVicerrector("Elena Muñoz");
    PeriodoAcademico periodo = escenario.crearPeriodoActivo("2026-2");
    cargador.cargarDemo(periodo, passwordEncoder.encode("Docentia2026*"));

    mockMvc
        .perform(
            post("/sesiones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new InicioSesionRequest(KAREN, "Docentia2026*"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.usuario.rol").value("ESTUDIANTE"))
        .andExpect(jsonPath("$.usuario.correo").value(KAREN));
  }

  @Test
  void cargarDosVecesNoDuplicaMatriculas() {
    PeriodoAcademico periodo = escenario.crearPeriodoActivo("2026-2");
    cargador.cargarDemo(periodo, "hash-de-prueba");
    cargador.cargarDemo(periodo, "hash-de-prueba");

    Usuario karen = usuario(KAREN);
    assertThat(matriculaRepository.findByEstudianteIdUsuarioAndEsActivaTrue(karen.getIdUsuario()))
        .hasSize(3);
  }

  private Usuario usuario(String correo) {
    return usuarioRepository.findByCorreoIgnoreCase(correo).orElseThrow();
  }
}
