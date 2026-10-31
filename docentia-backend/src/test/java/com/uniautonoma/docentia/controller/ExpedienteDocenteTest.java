package com.uniautonoma.docentia.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.NotasAspectos;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CompromisoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

class ExpedienteDocenteTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CompromisoRepository compromisoRepository;

  private Facultad ingenieria;
  private Facultad derecho;
  private Usuario docente;
  private PeriodoAcademico periodoActivo;

  @BeforeEach
  void crearEscenario() {
    ingenieria = escenario.crearFacultad("Ingeniería");
    derecho = escenario.crearFacultad("Derecho");
    docente =
        escenario.crearDocente(
            "Julian Hoyos", escenario.crearPrograma("Ingeniería de Software", ingenieria));
    periodoActivo = escenario.crearPeriodoActivo("2026-2");
  }

  @Test
  void muestraLaNotaDeCadaFuenteYDejaEstudiantesSinDatos() throws Exception {
    escenario.registrarCalificacion(
        docente,
        periodoActivo,
        FuenteEvaluacion.COORDINACION,
        notas("4.0", "3.0", "5.0", "4.0", "4.5"));
    escenario.registrarCalificacion(docente, periodoActivo, FuenteEvaluacion.VICERRECTORIA, "3.0");

    consultarComoCoordinador()
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.docente.nombreCompleto").value("Julian Hoyos"))
        .andExpect(jsonPath("$.periodo.nombre").value("2026-2"))
        .andExpect(jsonPath("$.notasPorFuente[0].fuente").value("ESTUDIANTES"))
        .andExpect(jsonPath("$.notasPorFuente[0].nota").doesNotExist())
        .andExpect(jsonPath("$.notasPorFuente[0].peso").value(50))
        .andExpect(jsonPath("$.notasPorFuente[1].fuente").value("COORDINACION"))
        .andExpect(jsonPath("$.notasPorFuente[1].nota").value(4.1))
        .andExpect(jsonPath("$.notasPorFuente[2].nota").value(3.0))
        .andExpect(jsonPath("$.promedio.estado").value("INCOMPLETO"))
        .andExpect(jsonPath("$.promedio.fuentesPendientes", contains("ESTUDIANTES")));
  }

  @Test
  void cadaDimensionTraeLaNotaPorFuenteYQuedaIncompletaSinEstudiantes() throws Exception {
    escenario.registrarCalificacion(
        docente,
        periodoActivo,
        FuenteEvaluacion.COORDINACION,
        notas("4.0", "3.0", "5.0", "4.0", "4.5"));
    escenario.registrarCalificacion(docente, periodoActivo, FuenteEvaluacion.VICERRECTORIA, "3.0");

    consultarComoCoordinador()
        .andExpect(jsonPath("$.dimensiones", hasSize(5)))
        .andExpect(jsonPath("$.dimensiones[0].dimension").value("DOMINIO_DISCIPLINAR"))
        .andExpect(jsonPath("$.dimensiones[0].nombre").value("Dominio disciplinar"))
        .andExpect(jsonPath("$.dimensiones[0].notaEstudiantes").doesNotExist())
        .andExpect(jsonPath("$.dimensiones[0].notaCoordinacion").value(4.0))
        .andExpect(jsonPath("$.dimensiones[0].notaVicerrectoria").value(3.0))
        .andExpect(jsonPath("$.dimensiones[0].estado").value("INCOMPLETO"))
        .andExpect(jsonPath("$.dimensiones[0].resultado").doesNotExist())
        .andExpect(jsonPath("$.dimensiones[2].notaCoordinacion").value(5.0));
  }

  @Test
  void conLasTresFuentesCadaDimensionTieneSuResultadoPonderado() throws Exception {
    escenario.registrarCalificacion(docente, periodoActivo, FuenteEvaluacion.ESTUDIANTES, "4.0");
    escenario.registrarCalificacion(
        docente,
        periodoActivo,
        FuenteEvaluacion.COORDINACION,
        notas("4.0", "3.0", "5.0", "4.0", "4.5"));
    escenario.registrarCalificacion(docente, periodoActivo, FuenteEvaluacion.VICERRECTORIA, "3.0");

    // Planeación académica: 4.0 * 0.5 + 3.0 * 0.3 + 3.0 * 0.2 = 3.5
    consultarComoCoordinador()
        .andExpect(jsonPath("$.dimensiones[1].dimension").value("PLANEACION_ACADEMICA"))
        .andExpect(jsonPath("$.dimensiones[1].estado").value("COMPLETO"))
        .andExpect(jsonPath("$.dimensiones[1].resultado").value(3.5))
        .andExpect(jsonPath("$.promedio.estado").value("COMPLETO"))
        .andExpect(jsonPath("$.promedio.valor").value(3.83));
  }

  @Test
  void elHistoricoTraeLosPeriodosAnterioresDelMasRecienteAlMasAntiguo() throws Exception {
    PeriodoAcademico primerSemestre =
        escenario.crearPeriodoCerrado("2026-1", LocalDate.of(2026, 2, 2));
    PeriodoAcademico anioAnterior =
        escenario.crearPeriodoCerrado("2025-2", LocalDate.of(2025, 8, 4));
    escenario.registrarCalificacion(docente, primerSemestre, FuenteEvaluacion.ESTUDIANTES, "4.0");
    escenario.registrarCalificacion(docente, primerSemestre, FuenteEvaluacion.COORDINACION, "3.0");
    escenario.registrarCalificacion(docente, primerSemestre, FuenteEvaluacion.VICERRECTORIA, "5.0");
    escenario.registrarCalificacion(docente, anioAnterior, FuenteEvaluacion.COORDINACION, "4.0");
    escenario.registrarCalificacion(docente, periodoActivo, FuenteEvaluacion.COORDINACION, "4.0");

    consultarComoCoordinador()
        .andExpect(jsonPath("$.historico[*].periodo.nombre", contains("2026-1", "2025-2")))
        .andExpect(jsonPath("$.historico[0].promedio.estado").value("COMPLETO"))
        .andExpect(jsonPath("$.historico[0].promedio.valor").value(3.9))
        .andExpect(jsonPath("$.historico[1].promedio.estado").value("INCOMPLETO"));
  }

  @Test
  void soloListaLosCompromisosVigentesOrdenadosPorFechaLimite() throws Exception {
    compromisoRepository.save(
        new Compromiso(
            docente, periodoActivo, "Actualizar el microcurrículo", LocalDate.of(2026, 11, 30)));
    compromisoRepository.save(
        new Compromiso(
            docente, periodoActivo, "Entregar notas a tiempo", LocalDate.of(2026, 10, 15)));
    Compromiso cumplido =
        new Compromiso(
            docente, periodoActivo, "Asistir a la capacitación", LocalDate.of(2026, 9, 1));
    cumplido.marcarCumplido();
    compromisoRepository.saveAndFlush(cumplido);

    consultarComoCoordinador()
        .andExpect(
            jsonPath(
                "$.compromisosVigentes[*].descripcion",
                contains("Entregar notas a tiempo", "Actualizar el microcurrículo")))
        .andExpect(jsonPath("$.compromisosVigentes[0].fechaLimite").value("2026-10-15"))
        .andExpect(jsonPath("$.compromisosVigentes[0].nombrePeriodo").value("2026-2"));
  }

  @Test
  void unDocenteSinCalificacionesTieneElExpedienteVacioPeroValido() throws Exception {
    consultarComoCoordinador()
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notasPorFuente", hasSize(3)))
        .andExpect(jsonPath("$.notasPorFuente[1].nota").doesNotExist())
        .andExpect(jsonPath("$.historico", empty()))
        .andExpect(jsonPath("$.compromisosVigentes", empty()));
  }

  @Test
  void elCoordinadorDeOtraFacultadNoEncuentraElExpediente() throws Exception {
    mockMvc
        .perform(
            get("/expedientes/{id}", docente.getIdUsuario())
                .with(comoCoordinador(derecho.getIdFacultad())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.mensaje").value("Docente no encontrado"));
  }

  @Test
  void elVicerrectorLoVeYElDocenteNo() throws Exception {
    mockMvc
        .perform(get("/expedientes/{id}", docente.getIdUsuario()).with(comoVicerrector()))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/expedientes/{id}", docente.getIdUsuario()).with(como(docente)))
        .andExpect(status().isForbidden());
  }

  private ResultActions consultarComoCoordinador() throws Exception {
    return mockMvc.perform(
        get("/expedientes/{id}", docente.getIdUsuario())
            .with(comoCoordinador(ingenieria.getIdFacultad())));
  }

  private NotasAspectos notas(
      String dominio, String planeacion, String metodologia, String evaluacion, String compromiso) {
    return new NotasAspectos(
        new BigDecimal(dominio),
        new BigDecimal(planeacion),
        new BigDecimal(metodologia),
        new BigDecimal(evaluacion),
        new BigDecimal(compromiso));
  }
}
