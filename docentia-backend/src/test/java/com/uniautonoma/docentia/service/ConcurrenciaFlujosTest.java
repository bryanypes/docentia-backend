package com.uniautonoma.docentia.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.EnvioEstudianteRequest;
import com.uniautonoma.docentia.dto.FormularioRequest;
import com.uniautonoma.docentia.dto.SolicitudAnalisisResponse;
import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.CitaHallazgo;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.Hallazgo;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.TipoHallazgo;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CitaRepository;
import com.uniautonoma.docentia.repository.FranjaHorariaRepository;
import com.uniautonoma.docentia.repository.SolicitudAnalisisRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.analisis.ValidacionHallazgoService;
import com.uniautonoma.docentia.service.formulario.FormularioService;
import com.uniautonoma.docentia.service.heteroevaluacion.HeteroevaluacionService;
import com.uniautonoma.docentia.service.heteroevaluacion.MatriculaService;
import com.uniautonoma.docentia.service.importacion.ImportacionService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Dos peticiones al mismo tiempo sobre lo mismo: una gana y nada queda duplicado. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConcurrenciaFlujosTest extends PruebaIntegracion {

  private static final String MARCA = "Concurrencia Flujos";
  private static final String PERIODO = "CF-2026";

  @Autowired private EscenarioPrueba escenario;
  @Autowired private ActaService actaService;
  @Autowired private ValidacionHallazgoService validacionService;
  @Autowired private ImportacionService importacionService;
  @Autowired private FormularioService formularioService;
  @Autowired private HeteroevaluacionService heteroevaluacion;
  @Autowired private MatriculaService matriculaService;
  @Autowired private SolicitudAnalisisRepository solicitudRepository;
  @Autowired private FranjaHorariaRepository franjaRepository;
  @Autowired private CitaRepository citaRepository;
  @Autowired private TransactionTemplate transaccion;
  @Autowired private JdbcTemplate jdbcTemplate;

  private Usuario docente;
  private Usuario coordinador;
  private PeriodoAcademico periodo;

  @BeforeEach
  void crearEscenario() {
    transaccion.executeWithoutResult(
        estado -> {
          Facultad facultad = escenario.crearFacultad("Facultad " + MARCA);
          docente =
              escenario.crearDocente(
                  "Docente " + MARCA, escenario.crearPrograma("Programa " + MARCA, facultad));
          coordinador = escenario.crearCoordinador("Coordinador " + MARCA, facultad);
          periodo = escenario.crearPeriodoActivo(PERIODO);
          for (TipoEtapa etapa : TipoEtapa.values()) {
            periodo =
                escenario.abrirEtapa(
                    periodo,
                    etapa,
                    BaseDatosPruebaConfig.HOY.minusDays(1),
                    BaseDatosPruebaConfig.HOY.plusDays(5));
          }
        });
  }

  @AfterEach
  void borrarLoCreado() {
    String enPeriodo =
        " where id_periodo in (select id_periodo from periodos_academicos where nombre = '"
            + PERIODO
            + "')";
    jdbcTemplate.update(
        "delete from firmas_acta where id_acta in (select id_acta from actas" + enPeriodo + ")");
    jdbcTemplate.update(
        "delete from versiones_hallazgo where id_hallazgo in (select id_hallazgo from"
            + " hallazgos_analisis where id_solicitud in (select id_solicitud from"
            + " solicitudes_analisis"
            + enPeriodo
            + "))");
    jdbcTemplate.update(
        "delete from evaluaciones_formulario where periodo_id in"
            + " (select id_periodo from periodos_academicos where nombre = '"
            + PERIODO
            + "')");
    jdbcTemplate.update(
        "delete from respuestas_heteroevaluacion where periodo_id in"
            + " (select id_periodo from periodos_academicos where nombre = '"
            + PERIODO
            + "')");
    jdbcTemplate.update(
        "delete from participaciones_heteroevaluacion where estudiante_id in"
            + " (select id_usuario from usuarios where nombre_completo like '%"
            + MARCA
            + "%' or correo = 'alumno.cf@uniautonoma.edu.co')");
    jdbcTemplate.update(
        "delete from matriculas where clase_id in (select id from clases where id_periodo in"
            + " (select id_periodo from periodos_academicos where nombre = '"
            + PERIODO
            + "'))");
    jdbcTemplate.update(
        "delete from clases_docente where clase_id in (select id from clases where id_periodo in"
            + " (select id_periodo from periodos_academicos where nombre = '"
            + PERIODO
            + "'))");
    jdbcTemplate.update(
        "delete from clases where id_periodo in"
            + " (select id_periodo from periodos_academicos where nombre = '"
            + PERIODO
            + "')");
    jdbcTemplate.update("delete from cursos where codigo like 'CF-%'");
    jdbcTemplate.update("delete from compromisos" + enPeriodo);
    jdbcTemplate.update("delete from actas" + enPeriodo);
    jdbcTemplate.update("delete from solicitudes_analisis" + enPeriodo);
    jdbcTemplate.update("delete from citas" + enPeriodo);
    jdbcTemplate.update("delete from franjas_horarias" + enPeriodo);
    jdbcTemplate.update("delete from comentarios_estudiantes" + enPeriodo);
    jdbcTemplate.update("delete from correcciones_calificacion" + enPeriodo);
    jdbcTemplate.update("delete from calificaciones" + enPeriodo);
    jdbcTemplate.update("delete from importaciones" + enPeriodo);
    jdbcTemplate.update("delete from promedios_docente" + enPeriodo);
    jdbcTemplate.update("delete from periodos_academicos where nombre = ?", PERIODO);
    jdbcTemplate.update("delete from usuarios where nombre_completo like ?", "%" + MARCA + "%");
    jdbcTemplate.update("delete from usuarios where correo = 'alumno.cf@uniautonoma.edu.co'");
    jdbcTemplate.update("delete from programas where nombre like ?", "%" + MARCA + "%");
    jdbcTemplate.update("delete from facultades where nombre like ?", "%" + MARCA + "%");
    jdbcTemplate.update("delete from cola_correo where notificacion_id is null");
  }

  private int enParalelo(Callable<?> tarea) throws Exception {
    int hilos = 2;
    CyclicBarrier salida = new CyclicBarrier(hilos);
    AtomicInteger exitos = new AtomicInteger();
    ExecutorService ejecutor = Executors.newFixedThreadPool(hilos);
    List<Future<?>> futuros = new ArrayList<>();
    for (int i = 0; i < hilos; i++) {
      futuros.add(
          ejecutor.submit(
              () -> {
                salida.await(10, TimeUnit.SECONDS);
                try {
                  tarea.call();
                  exitos.incrementAndGet();
                } catch (RuntimeException rechazo) {
                  // el perdedor recibe un error de negocio o de versión
                }
                return null;
              }));
    }
    for (Future<?> futuro : futuros) {
      futuro.get(30, TimeUnit.SECONDS);
    }
    ejecutor.shutdown();
    return exitos.get();
  }

  @Test
  void dobleFirmaSimultaneaDejaUnaSolaFirma() throws Exception {
    Long idActa =
        transaccion.execute(
            estado -> {
              for (FuenteEvaluacion fuente : FuenteEvaluacion.values()) {
                escenario.registrarCalificacion(docente, periodo, fuente, "4.0");
              }
              FranjaHoraria franja =
                  franjaRepository.saveAndFlush(
                      new FranjaHoraria(
                          escenario.recargar(coordinador),
                          periodo,
                          BaseDatosPruebaConfig.HOY.plusDays(1),
                          LocalTime.of(9, 0)));
              citaRepository.saveAndFlush(new Cita(franja, escenario.recargar(docente)));
              return actaService
                  .crear(docente.getIdUsuario(), UsuarioSesion.desde(coordinador))
                  .idActa();
            });
    UsuarioSesion sesion = UsuarioSesion.desde(coordinador);

    int exitos = enParalelo(() -> actaService.firmar(idActa, true, "127.0.0.1", sesion));

    assertThat(exitos).isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from firmas_acta where id_acta = ?", Integer.class, idActa))
        .isEqualTo(1);
  }

  @Test
  void dobleEnvioDelFormularioSoloGanaUno() throws Exception {
    UsuarioSesion sesion = UsuarioSesion.desde(docente);
    Map<String, BigDecimal> respuestas = new HashMap<>();
    for (int i = 1; i <= 14; i++) {
      respuestas.put(String.format("D%02d", i), BigDecimal.valueOf(4));
    }
    formularioService.guardarAutoevaluacion(null, new FormularioRequest(respuestas, null), sesion);

    int exitos = enParalelo(() -> formularioService.enviarAutoevaluacion(null, sesion));

    assertThat(exitos).isEqualTo(1);
  }

  @Test
  void dobleEnvioDelEstudianteYRecalculoConcurrenteNoSePisan() throws Exception {
    List<UsuarioSesion> alumnos = new ArrayList<>();
    Long asignacion =
        transaccion.execute(
            estado -> {
              var curso =
                  escenario.crearCurso(
                      "CF-101", "Curso " + MARCA, escenario.recargar(docente).getPrograma());
              var clase = escenario.crearClase(periodo, curso, "A", escenario.recargar(docente));
              for (int i = 0; i < 6; i++) {
                Usuario alumno =
                    escenario.crearEstudiante("Alumno " + MARCA + " " + i, curso.getPrograma());
                escenario.matricular(clase, alumno);
                alumnos.add(UsuarioSesion.desde(escenario.recargar(alumno)));
              }
              return jdbcTemplate.queryForObject(
                  "select id from clases_docente where clase_id = ?", Long.class, clase.getId());
            });
    Map<String, BigDecimal> respuestas = new HashMap<>();
    for (String codigo :
        List.of("D02", "D03", "D04", "D05", "D06", "D07", "D08", "D09", "D10", "D11", "D12")) {
      respuestas.put(codigo, BigDecimal.valueOf(4));
    }
    EnvioEstudianteRequest envio =
        new EnvioEstudianteRequest(respuestas, "El curso fue muy claro y útil.");
    UsuarioSesion primero = alumnos.get(0);

    int exitos =
        enParalelo(
            () -> {
              heteroevaluacion.enviar(asignacion, envio, primero);
              return null;
            });
    assertThat(exitos).isEqualTo(1);

    ExecutorService hilos = Executors.newFixedThreadPool(5);
    List<Future<?>> envios = new ArrayList<>();
    for (UsuarioSesion alumno : alumnos.subList(1, 6)) {
      envios.add(
          hilos.submit(
              () -> {
                heteroevaluacion.enviar(asignacion, envio, alumno);
                return null;
              }));
    }
    for (Future<?> futuro : envios) {
      futuro.get(30, TimeUnit.SECONDS);
    }
    hilos.shutdown();

    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from respuestas_heteroevaluacion where asignacion_id = ?",
                Long.class,
                asignacion))
        .isEqualTo(6);
    assertThat(
            jdbcTemplate.queryForObject(
                "select origen from calificaciones where id_docente = ? and fuente = 'ESTUDIANTES'",
                String.class,
                docente.getIdUsuario()))
        .isEqualTo("FORMULARIO");
  }

  @Test
  void dosImportacionesDeMatriculaSimultaneasNoDuplican() throws Exception {
    String csv =
        "correo_estudiante,nombre_estudiante,programa,codigo_curso,nombre_curso,correo_docente\n"
            + "alumno.cf@uniautonoma.edu.co,Alumno "
            + MARCA
            + ",Programa "
            + MARCA
            + ",CF-201,Curso "
            + MARCA
            + ","
            + docente.getCorreo()
            + "\n";
    UsuarioSesion sesion = UsuarioSesion.desde(coordinador);
    enParalelo(
        () ->
            matriculaService.importar(
                new MockMultipartFile(
                    "archivo", "m.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)),
                sesion));
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from matriculas m join usuarios u on u.id_usuario = m.estudiante_id"
                    + " where u.correo = 'alumno.cf@uniautonoma.edu.co'",
                Long.class))
        .isEqualTo(1);
  }

  @Test
  void doblePublicacionSoloGanaUna() throws Exception {
    Long idSolicitud =
        transaccion.execute(
            estado -> {
              Long idComentario =
                  escenario
                      .cargarComentarios(
                          docente, periodo, escenario.recargar(coordinador), List.of("Explica"))
                      .get(0);
              SolicitudAnalisis solicitud =
                  new SolicitudAnalisis(
                      escenario.recargar(docente), periodo, escenario.recargar(coordinador));
              Hallazgo hallazgo = new Hallazgo(1, TipoHallazgo.FORTALEZA, "Explica con ejemplos");
              hallazgo.agregarCita(new CitaHallazgo(idComentario, "Explica", true));
              hallazgo.validar(escenario.recargar(coordinador));
              solicitud.completar(List.of(hallazgo), 12, 12, "simulado", "v1");
              return solicitudRepository.saveAndFlush(solicitud).getIdSolicitud();
            });
    UsuarioSesion sesion = UsuarioSesion.desde(coordinador);

    int exitos =
        enParalelo(
            () -> {
              SolicitudAnalisisResponse respuesta = validacionService.publicar(idSolicitud, sesion);
              return respuesta;
            });

    assertThat(exitos).isEqualTo(1);
  }

  @Test
  void dosImportacionesSimultaneasNoDuplicanNada() throws Exception {
    String csv =
        "Correo docente;Dominio disciplinar;Planeación académica;Metodología de enseñanza;"
            + "Evaluación del aprendizaje;Compromiso institucional;Comentario\n"
            + docente.getCorreo()
            + ";4.0;4.0;4.0;4.0;4.0;Explica muy bien\n";
    UsuarioSesion sesion = UsuarioSesion.desde(coordinador);

    enParalelo(
        () ->
            importacionService.importar(
                new MockMultipartFile(
                    "archivo", "e.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)),
                null,
                null,
                sesion));

    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from comentarios_estudiantes where id_docente = ?",
                Integer.class,
                docente.getIdUsuario()))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from calificaciones where id_docente = ? and fuente = 'ESTUDIANTES'",
                Integer.class,
                docente.getIdUsuario()))
        .isEqualTo(1);
  }
}
