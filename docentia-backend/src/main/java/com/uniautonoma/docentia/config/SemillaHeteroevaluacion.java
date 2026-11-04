package com.uniautonoma.docentia.config;

import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.NotasAspectos;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.TipoFormulario;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.service.CalificacionService;
import com.uniautonoma.docentia.service.PromedioService;
import com.uniautonoma.docentia.service.formulario.CatalogoInstrumento;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Demo de heteroevaluación, solo en desarrollo: Laura tiene 6 estudiantes, 4 ya respondieron y 2
 * no. Con el quinto envío se alcanza el mínimo y su nota cambia en vivo. No toca a nadie más.
 */
@Component
@Profile("dev")
class SemillaHeteroevaluacion {

  private static final String DOMINIO = "@uniautonoma.edu.co";

  private final JdbcTemplate jdbcTemplate;
  private final UsuarioRepository usuarioRepository;
  private final PeriodoAcademicoRepository periodoRepository;
  private final CalificacionRepository calificacionRepository;
  private final CalificacionService calificacionService;
  private final PromedioService promedioService;
  private final CatalogoInstrumento catalogo;
  private final Clock reloj;
  private final SemillaFormularios semillaFormularios;

  SemillaHeteroevaluacion(
      JdbcTemplate jdbcTemplate,
      UsuarioRepository usuarioRepository,
      PeriodoAcademicoRepository periodoRepository,
      CalificacionRepository calificacionRepository,
      CalificacionService calificacionService,
      PromedioService promedioService,
      CatalogoInstrumento catalogo,
      Clock reloj,
      SemillaFormularios semillaFormularios) {
    this.semillaFormularios = semillaFormularios;
    this.jdbcTemplate = jdbcTemplate;
    this.usuarioRepository = usuarioRepository;
    this.periodoRepository = periodoRepository;
    this.calificacionRepository = calificacionRepository;
    this.calificacionService = calificacionService;
    this.promedioService = promedioService;
    this.catalogo = catalogo;
    this.reloj = reloj;
  }

  /** Siembra la demo si aún no existe y está el resto del seed de Ingeniería. */
  void cargarSiFalta(String hash) {
    if (usuarioRepository.findByCorreoIgnoreCase("laura.demo" + DOMINIO).isPresent()) {
      return;
    }
    Optional<PeriodoAcademico> periodo = periodoRepository.findByEsActivoTrue();
    Optional<Usuario> coordinador =
        usuarioRepository.findByCorreoIgnoreCase("coordinacion.ingenieria" + DOMINIO);
    Optional<Usuario> camilo = usuarioRepository.findByCorreoIgnoreCase("camilo.vidal" + DOMINIO);
    if (periodo.isEmpty() || coordinador.isEmpty() || camilo.isEmpty()) {
      return;
    }
    cargar(periodo.get(), camilo.get().getPrograma(), coordinador.get(), camilo.get(), hash);
  }

  void cargar(
      PeriodoAcademico periodo,
      Programa software,
      Usuario coordinador,
      Usuario otroDocente,
      String hash) {
    Usuario laura =
        usuarioRepository.save(
            Usuario.crearDocente("Laura Demo Estudiantes", "laura.demo" + DOMINIO, hash, software));
    calificar(laura, periodo, FuenteEvaluacion.COORDINACION, coordinador, "4.0");
    calificar(laura, periodo, FuenteEvaluacion.AUTOEVALUACION, laura, "4.0");
    calificacionService.registrarCalificacionEstudiantes(laura, periodo, notas("3.0"));

    Long asignacionLaura = asignar(periodo, software, "DEMO-101", "Programación web", laura);
    Long asignacionOtro = asignar(periodo, software, "DEMO-102", "Bases de datos", otroDocente);
    List<Usuario> estudiantes = new ArrayList<>();
    for (int i = 1; i <= 6; i++) {
      estudiantes.add(
          usuarioRepository.save(
              Usuario.crearEstudiante(
                  "Estudiante Demo " + i, "estudiante.demo" + i + DOMINIO, hash, software)));
    }
    estudiantes.forEach(estudiante -> matricular(asignacionLaura, estudiante));
    matricular(asignacionOtro, estudiantes.get(4));

    List<Long> descriptores =
        catalogo.de(TipoFormulario.HETERO).stream()
            .flatMap(c -> c.descriptores().stream())
            .map(CatalogoInstrumento.Descriptor::id)
            .toList();
    for (Usuario estudiante : estudiantes.subList(0, 4)) {
      jdbcTemplate.update(
          "insert into participaciones_heteroevaluacion (asignacion_id, estudiante_id, enviada_en)"
              + " values (?, ?, ?)",
          asignacionLaura,
          estudiante.getIdUsuario(),
          Timestamp.valueOf(LocalDateTime.now(reloj)));
      UUID respuesta =
          jdbcTemplate.queryForObject(
              "insert into respuestas_heteroevaluacion"
                  + " (asignacion_id, docente_id, periodo_id, instrumento_version)"
                  + " values (?, ?, ?, ?) returning id",
              UUID.class,
              asignacionLaura,
              laura.getIdUsuario(),
              periodo.getIdPeriodo(),
              CatalogoInstrumento.VERSION);
      descriptores.forEach(
          id ->
              jdbcTemplate.update(
                  "insert into respuestas_descriptor_hetero (respuesta_id, descriptor_id, valor)"
                      + " values (?, ?, 5)",
                  respuesta,
                  id));
    }
  }

  private Long asignar(
      PeriodoAcademico periodo, Programa programa, String codigo, String nombre, Usuario docente) {
    Long curso =
        jdbcTemplate.queryForObject(
            "insert into cursos (codigo, nombre, id_programa) values (?, ?, ?) returning id",
            Long.class,
            codigo,
            nombre,
            programa.getIdPrograma());
    Long clase =
        jdbcTemplate.queryForObject(
            "insert into clases (id_periodo, id_curso, grupo, jornada, tipo_curso)"
                + " values (?, ?, 'A', 'DIURNA', 'TEORICA') returning id",
            Long.class,
            periodo.getIdPeriodo(),
            curso);
    return jdbcTemplate.queryForObject(
        "insert into clases_docente (clase_id, docente_id) values (?, ?) returning id",
        Long.class,
        clase,
        docente.getIdUsuario());
  }

  private void matricular(Long asignacion, Usuario estudiante) {
    jdbcTemplate.update(
        "insert into matriculas (clase_id, estudiante_id)"
            + " select clase_id, ? from clases_docente where id = ?",
        estudiante.getIdUsuario(),
        asignacion);
  }

  /**
   * Coordinación y autoevaluación entran por su formulario enviado, como en la aplicación; solo los
   * estudiantes se registran directo.
   */
  private void calificar(
      Usuario docente,
      PeriodoAcademico periodo,
      FuenteEvaluacion fuente,
      Usuario evaluador,
      String nota) {
    if (fuente == FuenteEvaluacion.COORDINACION || fuente == FuenteEvaluacion.AUTOEVALUACION) {
      TipoFormulario tipo =
          fuente == FuenteEvaluacion.COORDINACION ? TipoFormulario.COEVAL : TipoFormulario.AUTO;
      semillaFormularios.enviar(docente, periodo, tipo, evaluador, nota);
      return;
    }
    Calificacion calificacion = new Calificacion(docente, periodo, fuente);
    calificacion.registrar(notas(nota), null, evaluador);
    calificacionRepository.saveAndFlush(calificacion);
    promedioService.recalcular(docente.getIdUsuario(), periodo.getIdPeriodo());
  }

  private static NotasAspectos notas(String nota) {
    BigDecimal valor = new BigDecimal(nota);
    return new NotasAspectos(valor, valor, valor, valor, valor);
  }
}
