package com.uniautonoma.docentia.config;

import com.uniautonoma.docentia.entity.Importacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.ResultadoImportacion;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.ComentarioEstudianteRepository;
import com.uniautonoma.docentia.repository.ImportacionRepository;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.service.importacion.ValidadorEvaluacionEstudiantes;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Comentarios de estudiantes de prueba para ensayar el análisis, solo en desarrollo. Cada docente
 * recibe temas que se repiten (puntualidad, claridad, retroalimentación) para que el análisis los
 * agrupe, y Julián también tiene comentarios del periodo anterior para que haya tendencias. Corre en
 * cada arranque sin duplicar: un docente que ya tiene sus comentarios se salta.
 */
@Component
@Profile("dev")
class SemillaComentarios {

  private static final String DOMINIO = "@uniautonoma.edu.co";

  private record Lote(String periodo, String docente, String coordinador, List<String> textos) {}

  private static final List<Lote> LOTES =
      List.of(
          new Lote(
              "2026-2",
              "diana.astaiza",
              "coordinacion.ingenieria",
              List.of(
                  "Es muy puntual, siempre llega a tiempo a clase.",
                  "Llega temprano y empieza la clase a la hora.",
                  "Nunca llega tarde, es muy responsable con el horario.",
                  "Explica con mucha claridad los temas difíciles.",
                  "Sus explicaciones son claras y usa buenos ejemplos.",
                  "Explica muy bien, se le entiende todo.",
                  "Podría dejar más ejercicios para practicar en casa.",
                  "Faltan ejercicios prácticos antes de los parciales.",
                  "La retroalimentación de los talleres llega rápido y es útil.",
                  "Siempre resuelve las dudas con paciencia.",
                  "Responde los correos y las dudas fuera de clase.",
                  "El material del curso está muy bien organizado.",
                  "A veces la clase se alarga más de la cuenta.")),
          new Lote(
              "2026-2",
              "camilo.vidal",
              "coordinacion.ingenieria",
              List.of(
                  "Excelente profesor, explica con claridad y paciencia.",
                  "Explica muy claro y con ejemplos de la industria.",
                  "Domina el tema y lo explica de forma sencilla.",
                  "Es puntual y organizado con el cronograma.",
                  "Siempre llega a tiempo y cumple el horario.",
                  "Califica rápido y da buena retroalimentación.",
                  "La retroalimentación de cada entrega es detallada.",
                  "Usa herramientas modernas y proyectos reales.",
                  "Las clases son dinámicas y participativas.",
                  "Motiva a investigar más allá de la clase.",
                  "Recomiendo mucho su materia.",
                  "A veces deja demasiadas tareas en la misma semana.")),
          new Lote(
              "2026-2",
              "nora.munoz",
              "coordinacion.ingenieria",
              List.of(
                  "Llega tarde a la mayoría de las clases.",
                  "Casi siempre empieza la clase quince minutos tarde.",
                  "No es puntual y a veces cancela la clase sin avisar.",
                  "No explica con claridad, solo lee las diapositivas.",
                  "Las explicaciones son confusas y no resuelve las dudas.",
                  "Cuesta entenderle, explica muy rápido.",
                  "Las notas se demoran mucho en publicarse.",
                  "No da retroalimentación de los trabajos.",
                  "La retroalimentación de los parciales nunca llega.",
                  "Los criterios de evaluación no son claros.",
                  "Es amable cuando uno le pregunta en la oficina.",
                  "El material del curso está desactualizado.")),
          new Lote(
              "2026-2",
              "ramiro.paz",
              "coordinacion.ingenieria",
              List.of(
                  "No llega a tiempo a las clases.",
                  "Llega tarde y se va antes de la hora.",
                  "No explica bien los laboratorios.",
                  "Las instrucciones de los laboratorios no son claras.",
                  "No publica las notas a tiempo.",
                  "Nunca da retroalimentación de los informes.",
                  "Los parciales no tienen que ver con lo visto en clase.",
                  "Las evaluaciones evalúan temas que no se explicaron.",
                  "Sabe mucho de electrónica.",
                  "Debería usar más herramientas de simulación.",
                  "La clase es muy monótona.")),
          new Lote(
              "2026-2",
              "hector.quilindo",
              "coordinacion.derecho",
              List.of(
                  "Explica los casos con mucha claridad.",
                  "Sus explicaciones de jurisprudencia son claras.",
                  "Usa ejemplos reales de casos que hacen fácil entender.",
                  "Es muy puntual con el horario.",
                  "Siempre llega a tiempo.",
                  "Fomenta el debate y el pensamiento crítico.",
                  "Las discusiones en clase nos hacen pensar.",
                  "Demora en entregar las notas de los parciales.",
                  "La retroalimentación de los ensayos llega tarde.",
                  "Deja lecturas demasiado largas para una semana.",
                  "Muy buen profesor, aprendí mucho.")),
          new Lote(
              "2026-2",
              "luisa.mosquera",
              "coordinacion.derecho",
              List.of(
                  "Es muy clara al explicar los procedimientos.",
                  "Explica con orden y claridad.",
                  "Siempre responde las dudas con paciencia.",
                  "Acompaña mucho el proceso de cada estudiante.",
                  "Hace seguimiento a nuestro avance en el semestre.",
                  "Llega puntual a todas las clases.",
                  "Los criterios de evaluación se explican desde el inicio.",
                  "Las evaluaciones están alineadas con lo que se ve en clase.",
                  "Podría usar más casos prácticos.",
                  "Faltan ejercicios prácticos para preparar los parciales.",
                  "Excelente docente.")),
          // Periodo anterior de Julián: con estos y su análisis de 2026-1 aparecen las tendencias.
          new Lote(
              "2026-1",
              "julian.hoyos",
              "coordinacion.ingenieria",
              List.of(
                  "Las notas se demoran muchísimo en salir.",
                  "La retroalimentación de los proyectos nunca llega a tiempo.",
                  "No da retroalimentación de los parciales.",
                  "Las notas de los cortes se publican tarde.",
                  "Explica con ejemplos reales, muy claro.",
                  "Sus explicaciones son claras.",
                  "Es puntual.",
                  "Los criterios de evaluación cambian durante el semestre.",
                  "Los talleres son muy útiles.",
                  "El ritmo de la clase es muy rápido.",
                  "Domina la arquitectura de software.")));

  private final UsuarioRepository usuarioRepository;
  private final PeriodoAcademicoRepository periodoRepository;
  private final ImportacionRepository importacionRepository;
  private final ComentarioEstudianteRepository comentarioRepository;

  SemillaComentarios(
      UsuarioRepository usuarioRepository,
      PeriodoAcademicoRepository periodoRepository,
      ImportacionRepository importacionRepository,
      ComentarioEstudianteRepository comentarioRepository) {
    this.usuarioRepository = usuarioRepository;
    this.periodoRepository = periodoRepository;
    this.importacionRepository = importacionRepository;
    this.comentarioRepository = comentarioRepository;
  }

  /** Carga los lotes que falten; los docentes o periodos que no existan se saltan. */
  void cargarSiFalta() {
    for (Lote lote : LOTES) {
      Optional<PeriodoAcademico> periodo = periodoRepository.findByNombre(lote.periodo());
      Optional<Usuario> docente = usuarioRepository.findByCorreoIgnoreCase(lote.docente() + DOMINIO);
      Optional<Usuario> coordinador =
          usuarioRepository.findByCorreoIgnoreCase(lote.coordinador() + DOMINIO);
      if (periodo.isEmpty() || docente.isEmpty() || coordinador.isEmpty()) {
        continue;
      }
      String primero = lote.textos().get(0);
      boolean yaCargado =
          comentarioRepository
              .findByDocenteIdUsuarioAndPeriodoIdPeriodoOrderByIdComentarioAsc(
                  docente.get().getIdUsuario(), periodo.get().getIdPeriodo())
              .stream()
              .anyMatch(c -> c.getTexto().equals(primero));
      if (!yaCargado) {
        cargar(periodo.get(), docente.get(), coordinador.get(), lote.textos());
      }
    }
  }

  private void cargar(
      PeriodoAcademico periodo, Usuario docente, Usuario coordinador, List<String> textos) {
    Importacion carga =
        importacionRepository.save(
            new Importacion(
                "seed-comentarios-prueba.csv",
                "seed-comentarios-" + docente.getCorreo() + "-" + periodo.getNombre(),
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
}
