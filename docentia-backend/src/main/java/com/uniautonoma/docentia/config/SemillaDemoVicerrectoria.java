package com.uniautonoma.docentia.config;

import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.Importacion;
import com.uniautonoma.docentia.entity.NotasAspectos;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.ResultadoImportacion;
import com.uniautonoma.docentia.entity.TipoFormulario;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.repository.ComentarioEstudianteRepository;
import com.uniautonoma.docentia.repository.ImportacionRepository;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.ProgramaRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.service.CalificacionService;
import com.uniautonoma.docentia.service.PromedioService;
import com.uniautonoma.docentia.service.formulario.CatalogoInstrumento;
import com.uniautonoma.docentia.service.heteroevaluacion.HeteroevaluacionService;
import com.uniautonoma.docentia.service.importacion.ValidadorEvaluacionEstudiantes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Datos para presentar a la vicerrectoría, solo en desarrollo.
 *
 * <p>Un flujo limpio de punta a punta: la docente Mariana Ruiz no tiene ninguna nota. Once
 * estudiantes ya la evaluaron con comentarios (alcanza para el análisis con IA) y el estudiante de
 * la demo es el que falta. En vivo: el estudiante evalúa, la docente se autoevalúa, la coordinación
 * la coevalúa y la vicerrectoría ve el resultado y el análisis de comentarios.
 *
 * <p>Además siembra 36 docentes en cinco programas con notas variadas (todos los niveles, unos
 * completos y otros a medias) para que las estadísticas se vean con volumen. No toca a nadie más.
 */
@Component
@Profile("dev")
class SemillaDemoVicerrectoria {

  private static final String DOMINIO = "@uniautonoma.edu.co";
  private static final String MARCA = "docente.flujo" + DOMINIO;

  /** Comentarios de los once estudiantes que ya evaluaron a la docente de la demo. */
  private static final List<String> COMENTARIOS =
      List.of(
          "Explica los requisitos con casos reales de empresas de la región y se entiende todo.",
          "Las clases son muy organizadas y siempre comparte el material antes.",
          "Las notas de los talleres tardan en publicarse, casi tres semanas.",
          "Responde las dudas con paciencia, incluso fuera de clase.",
          "El ritmo es rápido en los temas de modelado y cuesta seguirlo.",
          "Los ejercicios en grupo ayudan mucho a entender las historias de usuario.",
          "Los criterios de calificación del proyecto final no quedaron claros al inicio.",
          "Es puntual y llega con la clase preparada.",
          "Retroalimenta las entregas con comentarios concretos que sirven para mejorar.",
          "Podría dejar más ejercicios para practicar antes de los parciales.",
          "Recomiendo la materia, se aprende a trabajar con clientes de verdad.");

  /** Puntaje de cada uno de los once estudiantes, de 1 a 5, para una nota cercana a 4,1. */
  private static final int[] PUNTAJES = {5, 4, 4, 5, 3, 4, 4, 5, 4, 4, 4};

  private final JdbcTemplate jdbcTemplate;
  private final UsuarioRepository usuarioRepository;
  private final PeriodoAcademicoRepository periodoRepository;
  private final ProgramaRepository programaRepository;
  private final CalificacionRepository calificacionRepository;
  private final CalificacionService calificacionService;
  private final PromedioService promedioService;
  private final HeteroevaluacionService heteroevaluacionService;
  private final CatalogoInstrumento catalogo;
  private final ImportacionRepository importacionRepository;
  private final ComentarioEstudianteRepository comentarioRepository;
  private final Clock reloj;

  SemillaDemoVicerrectoria(
      JdbcTemplate jdbcTemplate,
      UsuarioRepository usuarioRepository,
      PeriodoAcademicoRepository periodoRepository,
      ProgramaRepository programaRepository,
      CalificacionRepository calificacionRepository,
      CalificacionService calificacionService,
      PromedioService promedioService,
      HeteroevaluacionService heteroevaluacionService,
      CatalogoInstrumento catalogo,
      ImportacionRepository importacionRepository,
      ComentarioEstudianteRepository comentarioRepository,
      Clock reloj) {
    this.jdbcTemplate = jdbcTemplate;
    this.usuarioRepository = usuarioRepository;
    this.periodoRepository = periodoRepository;
    this.programaRepository = programaRepository;
    this.calificacionRepository = calificacionRepository;
    this.calificacionService = calificacionService;
    this.promedioService = promedioService;
    this.heteroevaluacionService = heteroevaluacionService;
    this.catalogo = catalogo;
    this.importacionRepository = importacionRepository;
    this.comentarioRepository = comentarioRepository;
    this.reloj = reloj;
  }

  /** Siembra la demo si aún no existe y ya está el seed de Ingeniería y Derecho. */
  void cargarSiFalta(String hash) {
    if (usuarioRepository.findByCorreoIgnoreCase(MARCA).isPresent()) {
      return;
    }
    Optional<PeriodoAcademico> periodo = periodoRepository.findByEsActivoTrue();
    Optional<Usuario> coordIngenieria =
        usuarioRepository.findByCorreoIgnoreCase("coordinacion.ingenieria" + DOMINIO);
    Optional<Usuario> coordDerecho =
        usuarioRepository.findByCorreoIgnoreCase("coordinacion.derecho" + DOMINIO);
    Optional<Programa> software =
        programaRepository.findFirstByNombreIgnoreCaseAndEsActivoTrue("Ingeniería de Software");
    Optional<Programa> electronica =
        programaRepository.findFirstByNombreIgnoreCaseAndEsActivoTrue("Ingeniería Electrónica");
    Optional<Programa> derecho =
        programaRepository.findFirstByNombreIgnoreCaseAndEsActivoTrue("Derecho");
    if (periodo.isEmpty()
        || coordIngenieria.isEmpty()
        || coordDerecho.isEmpty()
        || software.isEmpty()
        || electronica.isEmpty()
        || derecho.isEmpty()) {
      return;
    }
    sembrarFlujo(periodo.get(), software.get(), coordIngenieria.get(), hash);
    Programa ambiental =
        programaRepository.save(new Programa("Ingeniería Ambiental", software.get().getFacultad()));
    Programa politica =
        programaRepository.save(new Programa("Ciencia Política", derecho.get().getFacultad()));
    sembrarVolumen(
        periodo.get(),
        List.of(software.get(), electronica.get(), ambiental, derecho.get(), politica),
        coordIngenieria.get(),
        coordDerecho.get(),
        hash);
  }

  private void sembrarFlujo(
      PeriodoAcademico periodo, Programa software, Usuario coordinador, String hash) {
    Usuario docente =
        usuarioRepository.save(Usuario.crearDocente("Mariana Ruiz Ortega", MARCA, hash, software));
    Usuario estudianteDemo =
        usuarioRepository.save(
            Usuario.crearEstudiante(
                "Santiago Gómez Ríos", "estudiante.flujo" + DOMINIO, hash, software));
    Long asignacion = asignar(periodo, software, "FLUJO-301", "Ingeniería de Requisitos", docente);
    matricular(asignacion, estudianteDemo);
    List<Long> descriptores =
        catalogo.de(TipoFormulario.HETERO).stream()
            .flatMap(c -> c.descriptores().stream())
            .map(CatalogoInstrumento.Descriptor::id)
            .toList();
    for (int i = 0; i < COMENTARIOS.size(); i++) {
      Usuario estudiante =
          usuarioRepository.save(
              Usuario.crearEstudiante(
                  "Estudiante Requisitos " + (i + 1),
                  "estudiante.requisitos" + (i + 1) + DOMINIO,
                  hash,
                  software));
      matricular(asignacion, estudiante);
      responder(
          asignacion, estudiante, docente, periodo, descriptores, PUNTAJES[i], COMENTARIOS.get(i));
    }
    // La nota de estudiantes sale por el mismo camino que un envío real.
    heteroevaluacionService.recalcular(docente, periodo);
    // El análisis con IA que se pide desde la pantalla lee los comentarios importados de la
    // encuesta institucional (fuente CSV), igual que en el seed de Julián: se cargan los mismos
    // comentarios más uno, para superar el mínimo de 10 encuestas.
    List<String> encuesta = new java.util.ArrayList<>(COMENTARIOS);
    encuesta.add(
        "Sus clases son claras y las prácticas con clientes reales ayudan mucho a aprender.");
    cargarComentarios(coordinador, periodo, docente, encuesta);
  }

  private void cargarComentarios(
      Usuario coordinador, PeriodoAcademico periodo, Usuario docente, List<String> textos) {
    Importacion carga =
        importacionRepository.save(
            new Importacion(
                "seed-evaluacion-estudiantes.csv",
                "seed-" + docente.getCorreo(),
                periodo,
                coordinador,
                ResultadoImportacion.EXITOSA,
                "UTF-8",
                ";",
                textos.size(),
                0));
    importacionRepository.flush();
    for (String texto : textos) {
      comentarioRepository.insertarSiNoExiste(
          docente.getIdUsuario(),
          periodo.getIdPeriodo(),
          texto,
          ValidadorEvaluacionEstudiantes.huellaComentario(texto, 1),
          carga.getIdImportacion());
    }
  }

  private void responder(
      Long asignacion,
      Usuario estudiante,
      Usuario docente,
      PeriodoAcademico periodo,
      List<Long> descriptores,
      int puntaje,
      String comentario) {
    jdbcTemplate.update(
        "insert into participaciones_heteroevaluacion (asignacion_id, estudiante_id, enviada_en)"
            + " values (?, ?, ?)",
        asignacion,
        estudiante.getIdUsuario(),
        Timestamp.valueOf(LocalDateTime.now(reloj)));
    UUID respuesta =
        jdbcTemplate.queryForObject(
            "insert into respuestas_heteroevaluacion"
                + " (asignacion_id, docente_id, periodo_id, instrumento_version, comentario)"
                + " values (?, ?, ?, ?, ?) returning id",
            UUID.class,
            asignacion,
            docente.getIdUsuario(),
            periodo.getIdPeriodo(),
            CatalogoInstrumento.VERSION,
            comentario);
    for (int i = 0; i < descriptores.size(); i++) {
      // Un punto menos en un descriptor de cada tres para que no todo sea igual.
      int valor = Math.max(1, i % 3 == 2 ? puntaje - 1 : puntaje);
      jdbcTemplate.update(
          "insert into respuestas_descriptor_hetero (respuesta_id, descriptor_id, valor)"
              + " values (?, ?, ?)",
          respuesta,
          descriptores.get(i),
          valor);
    }
  }

  /**
   * 36 docentes con notas deterministas: todos los niveles de la Resolución 074, unos pocos bajo el
   * umbral, y uno de cada cinco con alguna fuente pendiente para que el avance no sea 100 %.
   */
  private void sembrarVolumen(
      PeriodoAcademico periodo,
      List<Programa> programas,
      Usuario coordIngenieria,
      Usuario coordDerecho,
      String hash) {
    String[] nombres = {
      "Alejandro", "Beatriz", "Carlos", "Daniela", "Eduardo", "Fernanda", "Gustavo", "Helena",
      "Iván", "Juliana", "Kevin", "Lorena", "Mauricio", "Natalia", "Orlando", "Patricia",
      "Ricardo", "Sandra", "Tomás", "Valentina", "Wilson", "Ximena", "Yesid", "Zulma",
      "Andrea", "Bernardo", "Claudia", "Diego", "Esperanza", "Felipe", "Gloria", "Hernán",
      "Isabel", "Jorge", "Karina", "Leonardo"
    };
    String[] apellidos = {
      "Bolaños", "Cerón", "Daza", "Fernández", "Gironza", "Hurtado", "Idrobo", "Jiménez", "López"
    };
    // Medias de cada docente: recorren la escala y se repiten con pequeñas variaciones.
    double[] medias = {4.7, 4.4, 4.2, 3.9, 4.6, 3.6, 4.1, 4.3, 3.3, 4.5, 3.95, 2.9};
    for (int i = 0; i < nombres.length; i++) {
      Programa programa = programas.get(i % programas.size());
      boolean esDerecho =
          programa.getFacultad().getIdFacultad().equals(coordDerecho.getFacultad().getIdFacultad());
      Usuario coordinador = esDerecho ? coordDerecho : coordIngenieria;
      String nombre =
          nombres[i]
              + " "
              + apellidos[i % apellidos.length]
              + " "
              + apellidos[(i + 4) % apellidos.length];
      Usuario docente =
          usuarioRepository.save(
              Usuario.crearDocente(
                  nombre, "docente.estadistica" + (i + 1) + DOMINIO, hash, programa));
      double media = medias[i % medias.length] - (i / medias.length) * 0.05;
      String estudiantes = nota(media + 0.1);
      String coordinacion = nota(media);
      String autoevaluacion = nota(Math.min(5.0, media + 0.2));
      calificar(docente, periodo, FuenteEvaluacion.COORDINACION, coordinador, coordinacion);
      if (i % 5 == 3) {
        continue; // falta autoevaluación y estudiantes: caso "en curso"
      }
      calificar(docente, periodo, FuenteEvaluacion.AUTOEVALUACION, docente, autoevaluacion);
      if (i % 5 == 4 && i % 2 == 0) {
        continue; // falta la nota de estudiantes
      }
      calificacionService.registrarCalificacionEstudiantes(docente, periodo, notas(estudiantes));
    }
  }

  private static String nota(double valor) {
    double acotado = Math.max(1.0, Math.min(5.0, valor));
    return BigDecimal.valueOf(acotado).setScale(1, RoundingMode.HALF_UP).toPlainString();
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

  private void calificar(
      Usuario docente,
      PeriodoAcademico periodo,
      FuenteEvaluacion fuente,
      Usuario evaluador,
      String nota) {
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
