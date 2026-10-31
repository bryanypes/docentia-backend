package com.uniautonoma.docentia;

import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.ComentarioEstudiante;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.Importacion;
import com.uniautonoma.docentia.entity.NotasAspectos;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.ResultadoImportacion;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.repository.ComentarioEstudianteRepository;
import com.uniautonoma.docentia.repository.FacultadRepository;
import com.uniautonoma.docentia.repository.ImportacionRepository;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.ProgramaRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.service.PromedioService;
import com.uniautonoma.docentia.service.importacion.ValidadorEvaluacionEstudiantes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Crea los datos que se repiten entre pruebas: usuarios por rol, periodos y calificaciones. */
@Component
public class EscenarioPrueba {

  private static final String HASH = "hash-de-prueba";

  private final FacultadRepository facultadRepository;
  private final ProgramaRepository programaRepository;
  private final UsuarioRepository usuarioRepository;
  private final PeriodoAcademicoRepository periodoRepository;
  private final CalificacionRepository calificacionRepository;
  private final PromedioService promedioService;
  private final ImportacionRepository importacionRepository;
  private final ComentarioEstudianteRepository comentarioRepository;

  public EscenarioPrueba(
      FacultadRepository facultadRepository,
      ProgramaRepository programaRepository,
      UsuarioRepository usuarioRepository,
      PeriodoAcademicoRepository periodoRepository,
      CalificacionRepository calificacionRepository,
      PromedioService promedioService,
      ImportacionRepository importacionRepository,
      ComentarioEstudianteRepository comentarioRepository) {
    this.promedioService = promedioService;
    this.importacionRepository = importacionRepository;
    this.comentarioRepository = comentarioRepository;
    this.facultadRepository = facultadRepository;
    this.programaRepository = programaRepository;
    this.usuarioRepository = usuarioRepository;
    this.periodoRepository = periodoRepository;
    this.calificacionRepository = calificacionRepository;
  }

  /**
   * Carga comentarios de estudiantes como si vinieran de un CSV importado por el coordinador.
   *
   * @return los identificadores de los comentarios en el orden recibido
   */
  public List<Long> cargarComentarios(
      Usuario docente, PeriodoAcademico periodo, Usuario coordinador, List<String> textos) {
    Importacion importacion =
        importacionRepository.saveAndFlush(
            new Importacion(
                "prueba.csv",
                "0".repeat(64),
                periodo,
                coordinador,
                ResultadoImportacion.EXITOSA,
                "UTF-8",
                ";",
                textos.size(),
                0));
    Map<String, Integer> apariciones = new HashMap<>();
    for (String texto : textos) {
      int aparicion = apariciones.merge(texto, 1, Integer::sum);
      comentarioRepository.insertarSiNoExiste(
          docente.getIdUsuario(),
          periodo.getIdPeriodo(),
          texto,
          ValidadorEvaluacionEstudiantes.huellaComentario(texto, aparicion),
          importacion.getIdImportacion());
    }
    return comentarioRepository
        .findByDocenteIdUsuarioAndPeriodoIdPeriodoOrderByIdComentarioAsc(
            docente.getIdUsuario(), periodo.getIdPeriodo())
        .stream()
        .map(ComentarioEstudiante::getIdComentario)
        .toList();
  }

  /** Programa o reprograma una etapa del periodo. */
  public PeriodoAcademico abrirEtapa(
      PeriodoAcademico periodo, TipoEtapa etapa, LocalDate apertura, LocalDate cierre) {
    periodo.programarEtapa(etapa, apertura, cierre);
    return periodoRepository.saveAndFlush(periodo);
  }

  public Facultad crearFacultad(String nombre) {
    return facultadRepository.save(new Facultad(nombre));
  }

  public Programa crearPrograma(String nombre, Facultad facultad) {
    return programaRepository.save(new Programa(nombre, facultad));
  }

  public Usuario crearDocente(String nombre, Programa programa) {
    return usuarioRepository.save(Usuario.crearDocente(nombre, correoDe(nombre), HASH, programa));
  }

  public Usuario crearCoordinador(String nombre, Facultad facultad) {
    return usuarioRepository.save(
        Usuario.crearCoordinador(nombre, correoDe(nombre), HASH, facultad));
  }

  public Usuario crearVicerrector(String nombre) {
    return usuarioRepository.save(Usuario.crearVicerrector(nombre, correoDe(nombre), HASH));
  }

  public PeriodoAcademico crearPeriodoActivo(String nombre) {
    PeriodoAcademico periodo = crearPeriodo(nombre, LocalDate.of(2026, 8, 3));
    // La recolección cubre todo el periodo, así que incluye el día fijo de las pruebas.
    periodo.programarEtapa(
        TipoEtapa.RECOLECCION_CALIFICACIONES, periodo.getFechaInicio(), periodo.getFechaFin());
    periodo.setEsActivo(true);
    return periodoRepository.saveAndFlush(periodo);
  }

  /** Crea el periodo activo con la recolección abierta solo entre las fechas indicadas. */
  public PeriodoAcademico crearPeriodoActivoConRecoleccion(
      String nombre, LocalDate apertura, LocalDate cierre) {
    PeriodoAcademico periodo = crearPeriodo(nombre, LocalDate.of(2026, 8, 3));
    periodo.programarEtapa(TipoEtapa.RECOLECCION_CALIFICACIONES, apertura, cierre);
    periodo.setEsActivo(true);
    return periodoRepository.saveAndFlush(periodo);
  }

  public PeriodoAcademico crearPeriodoCerrado(String nombre, LocalDate fechaInicio) {
    return periodoRepository.saveAndFlush(crearPeriodo(nombre, fechaInicio));
  }

  /** Registra una calificación con la misma nota en los cinco aspectos. */
  public Calificacion registrarCalificacion(
      Usuario docente, PeriodoAcademico periodo, FuenteEvaluacion fuente, String nota) {
    BigDecimal valor = new BigDecimal(nota);
    return registrarCalificacion(
        docente, periodo, fuente, new NotasAspectos(valor, valor, valor, valor, valor));
  }

  public Calificacion registrarCalificacion(
      Usuario docente, PeriodoAcademico periodo, FuenteEvaluacion fuente, NotasAspectos notas) {
    Calificacion calificacion = new Calificacion(docente, periodo, fuente);
    calificacion.registrar(notas, "Observación reservada de " + fuente, null);
    Calificacion guardada = calificacionRepository.saveAndFlush(calificacion);
    // Igual que al calificar por la API: cada calificación deja el promedio al día.
    promedioService.recalcular(docente.getIdUsuario(), periodo.getIdPeriodo());
    return guardada;
  }

  /** Guarda una calificación sin calcular el promedio, como si viniera de una base anterior. */
  public Calificacion guardarCalificacionSinPromedio(
      Usuario docente, PeriodoAcademico periodo, FuenteEvaluacion fuente, String nota) {
    BigDecimal valor = new BigDecimal(nota);
    Calificacion calificacion = new Calificacion(docente, periodo, fuente);
    calificacion.registrar(new NotasAspectos(valor, valor, valor, valor, valor), null, null);
    return calificacionRepository.saveAndFlush(calificacion);
  }

  private PeriodoAcademico crearPeriodo(String nombre, LocalDate fechaInicio) {
    return new PeriodoAcademico(nombre, fechaInicio, fechaInicio.plusMonths(4));
  }

  private String correoDe(String nombre) {
    return nombre.toLowerCase().replace(' ', '.') + "@uniautonoma.edu.co";
  }
}
