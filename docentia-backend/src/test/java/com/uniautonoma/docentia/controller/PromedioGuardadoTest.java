package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.CalificacionRequest;
import com.uniautonoma.docentia.dto.EstadoPromedio;
import com.uniautonoma.docentia.dto.PonderacionRequest;
import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.NotasAspectos;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.PromedioDocente;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.repository.PromedioDocenteRepository;
import com.uniautonoma.docentia.service.CalificacionService;
import com.uniautonoma.docentia.service.PromedioService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

/** El promedio se guarda al calificar y al cambiar ponderaciones, y las consultas lo leen. */
class PromedioGuardadoTest extends PruebaIntegracion {

  private static final String RUTA_COORDINACION = "/docentes/{id}/calificaciones/coordinacion";
  private static final String RUTA_VICERRECTORIA = "/docentes/{id}/calificaciones/vicerrectoria";

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CalificacionService calificacionService;
  @Autowired private PromedioService promedioService;
  @Autowired private PromedioDocenteRepository promedioRepository;
  @Autowired private CalificacionRepository calificacionRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManager entityManager;

  private Facultad ingenieria;
  private PeriodoAcademico periodo;
  private Usuario docente;
  private Usuario coordinador;
  private Usuario vicerrector;

  @BeforeEach
  void crearEscenario() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    docente =
        escenario.crearDocente(
            "Julian Hoyos", escenario.crearPrograma("Ingeniería de Software", ingenieria));
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    vicerrector = escenario.crearVicerrector("Elena Munoz");
    periodo = escenario.crearPeriodoActivo("2026-2");
  }

  @Test
  void calificarGuardaElPromedioIncompletoConLasFuentesPendientes() throws Exception {
    calificar(coordinador, RUTA_COORDINACION, docente, "3.0").andExpect(status().isOk());

    PromedioDocente guardado = promedioGuardado(docente);
    assertThat(guardado.getEstado()).isEqualTo(EstadoPromedio.INCOMPLETO);
    assertThat(guardado.getValor()).isNull();
    assertThat(guardado.getFuentesPendientes())
        .containsExactly(FuenteEvaluacion.ESTUDIANTES, FuenteEvaluacion.VICERRECTORIA);
    assertThat(guardado.getFechaCalculo()).isNotNull();
  }

  @Test
  void conLasTresFuentesSeGuardaElPromedioCompleto() throws Exception {
    registrarLasTresFuentes(docente, "4.0", "3.0", "5.0");

    // 4.0 * 50% + 3.0 * 30% + 5.0 * 20%
    PromedioDocente guardado = promedioGuardado(docente);
    assertThat(guardado.getEstado()).isEqualTo(EstadoPromedio.COMPLETO);
    assertThat(guardado.getValor()).isEqualByComparingTo("3.90");
    assertThat(guardado.getFuentesPendientes()).isEmpty();
  }

  @Test
  void corregirUnaNotaRecalculaElPromedioGuardado() throws Exception {
    registrarLasTresFuentes(docente, "4.0", "3.0", "5.0");

    calificar(coordinador, RUTA_COORDINACION, docente, "5.0").andExpect(status().isOk());

    // 4.0 * 50% + 5.0 * 30% + 5.0 * 20%
    assertThat(promedioGuardado(docente).getValor()).isEqualByComparingTo("4.50");
    assertThat(promedioRepository.count()).isEqualTo(1);
    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(jsonPath("$.promedio.valor").value(4.5));
  }

  @Test
  void corregirLaNotaDeEstudiantesRecalculaElPromedioGuardado() throws Exception {
    registrarLasTresFuentes(docente, "4.0", "3.0", "5.0");

    calificacionService.registrarCalificacionEstudiantes(docente, periodo, notasIguales("2.0"));

    // 2.0 * 50% + 3.0 * 30% + 5.0 * 20%
    assertThat(promedioGuardado(docente).getValor()).isEqualByComparingTo("2.90");
  }

  @Test
  void cambiarLasPonderacionesRecalculaLosPromediosDelPeriodo() throws Exception {
    Usuario otroDocente =
        escenario.crearDocente(
            "Diana Astaiza", escenario.crearPrograma("Ingeniería Electrónica", ingenieria));
    registrarLasTresFuentes(docente, "4.0", "3.0", "5.0");
    registrarLasTresFuentes(otroDocente, "5.0", "3.0", "4.0");

    mockMvc
        .perform(
            put("/periodos-academicos/activo/ponderaciones")
                .with(comoVicerrector())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    aJson(
                        new PonderacionRequest(
                            new BigDecimal("40"), new BigDecimal("40"), new BigDecimal("20")))))
        .andExpect(status().isOk());

    // 4.0 * 40% + 3.0 * 40% + 5.0 * 20%  y  5.0 * 40% + 3.0 * 40% + 4.0 * 20%
    assertThat(promedioGuardado(docente).getValor()).isEqualByComparingTo("3.80");
    assertThat(promedioGuardado(otroDocente).getValor()).isEqualByComparingTo("4.00");
  }

  @Test
  void laConsultaLeeElPromedioGuardadoSinRecalcularlo() throws Exception {
    registrarLasTresFuentes(docente, "4.0", "3.0", "5.0");
    // Un valor que ninguna combinación de las notas produce: solo puede salir de la tabla.
    entityManager.flush();
    jdbcTemplate.update(
        "update promedios_docente set valor = 1.11 where id_docente = ?", docente.getIdUsuario());
    entityManager.clear();

    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(jsonPath("$.promedio.valor").value(1.11));
    mockMvc
        .perform(get("/expedientes/{id}", docente.getIdUsuario()).with(comoVicerrector()))
        .andExpect(jsonPath("$.promedio.valor").value(1.11));
  }

  @Test
  void sinCalificacionesEnElPeriodoElPromedioEsIncompletoConLasTresFuentes() throws Exception {
    mockMvc
        .perform(get("/expedientes/propio").with(como(docente)))
        .andExpect(jsonPath("$.promedio.estado").value("INCOMPLETO"))
        .andExpect(
            jsonPath(
                "$.promedio.fuentesPendientes",
                contains("ESTUDIANTES", "COORDINACION", "VICERRECTORIA")));
    assertThat(promedioRepository.count()).isZero();
  }

  @Test
  void seCalculanLosPromediosDeCalificacionesQueNoTienenUno() {
    // Una calificación guardada sin pasar por el servicio, como las que había antes de la V9.
    Calificacion calificacion = new Calificacion(docente, periodo, FuenteEvaluacion.COORDINACION);
    calificacion.registrar(notasIguales("4.0"), null, coordinador);
    calificacionRepository.saveAndFlush(calificacion);

    assertThat(promedioService.recalcularPendientes()).isEqualTo(1);
    assertThat(promedioGuardado(docente).getEstado()).isEqualTo(EstadoPromedio.INCOMPLETO);
    assertThat(promedioService.recalcularPendientes()).isZero();
  }

  @Test
  void unCoordinadorDeOtraFacultadNoCalificaNiGeneraPromedio() throws Exception {
    Facultad derecho = escenario.crearFacultad("Derecho");
    Usuario docenteDerecho =
        escenario.crearDocente("Luisa Mosquera", escenario.crearPrograma("Derecho", derecho));

    calificar(coordinador, RUTA_COORDINACION, docenteDerecho, "4.0")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("Docente no encontrado"));
    mockMvc
        .perform(get("/expedientes/{id}", docenteDerecho.getIdUsuario()).with(como(coordinador)))
        .andExpect(status().isNotFound());
    assertThat(
            promedioRepository.findByDocenteIdUsuarioAndPeriodoIdPeriodo(
                docenteDerecho.getIdUsuario(), periodo.getIdPeriodo()))
        .isEmpty();
  }

  @Test
  void conElPromedioCompletoElDocenteSigueSinVerLasNotasDeCadaFuente() throws Exception {
    // Valores poco comunes a propósito: si alguno aparece en la respuesta es porque se filtró.
    registrarLasTresFuentes(docente, "3.9", "4.7", "2.3");

    String respuesta =
        mockMvc
            .perform(get("/expedientes/propio").with(como(docente)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.promedio.estado").value("COMPLETO"))
            // 3.9 * 50% + 4.7 * 30% + 2.3 * 20%
            .andExpect(jsonPath("$.promedio.valor").value(3.82))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(respuesta)
        .doesNotContain("3.9", "4.7", "2.3")
        .doesNotContain("notasPorFuente", "notaEstudiantes", "notaCoordinacion")
        .doesNotContain("notaVicerrectoria", "dominioDisciplinar", "observacion", "evaluador");
  }

  private void registrarLasTresFuentes(
      Usuario calificado, String estudiantes, String coordinacion, String vicerrectoria)
      throws Exception {
    calificacionService.registrarCalificacionEstudiantes(
        calificado, periodo, notasIguales(estudiantes));
    calificar(coordinador, RUTA_COORDINACION, calificado, coordinacion).andExpect(status().isOk());
    calificar(vicerrector, RUTA_VICERRECTORIA, calificado, vicerrectoria)
        .andExpect(status().isOk());
  }

  private ResultActions calificar(Usuario evaluador, String ruta, Usuario calificado, String nota)
      throws Exception {
    BigDecimal valor = new BigDecimal(nota);
    CalificacionRequest solicitud =
        new CalificacionRequest(valor, valor, valor, valor, valor, null);
    return mockMvc.perform(
        put(ruta, calificado.getIdUsuario())
            .with(como(evaluador))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(solicitud)));
  }

  private PromedioDocente promedioGuardado(Usuario calificado) {
    return promedioRepository
        .findByDocenteIdUsuarioAndPeriodoIdPeriodo(
            calificado.getIdUsuario(), periodo.getIdPeriodo())
        .orElseThrow();
  }

  private NotasAspectos notasIguales(String nota) {
    BigDecimal valor = new BigDecimal(nota);
    return new NotasAspectos(valor, valor, valor, valor, valor);
  }
}
