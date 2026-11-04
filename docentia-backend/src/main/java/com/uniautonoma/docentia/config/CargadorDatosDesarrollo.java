package com.uniautonoma.docentia.config;

import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.Importacion;
import com.uniautonoma.docentia.entity.NotasAspectos;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.ResultadoImportacion;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.TipoFormulario;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.repository.ComentarioEstudianteRepository;
import com.uniautonoma.docentia.repository.CompromisoRepository;
import com.uniautonoma.docentia.repository.FacultadRepository;
import com.uniautonoma.docentia.repository.FranjaHorariaRepository;
import com.uniautonoma.docentia.repository.ImportacionRepository;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.ProgramaRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.service.CalificacionService;
import com.uniautonoma.docentia.service.PromedioService;
import com.uniautonoma.docentia.service.campus.CargadorClasesCampus;
import com.uniautonoma.docentia.service.importacion.ValidadorEvaluacionEstudiantes;
import com.uniautonoma.docentia.service.notificacion.SilencioNotificaciones;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Crea usuarios y datos de ejemplo para trabajar en local mientras no exista el SSO. Solo corre con
 * el perfil dev y sobre una base sin usuarios.
 */
@Component
@Profile("dev")
public class CargadorDatosDesarrollo implements CommandLineRunner {

  private static final String DOMINIO_CORREO = "@uniautonoma.edu.co";

  private final UsuarioRepository usuarioRepository;
  private final FacultadRepository facultadRepository;
  private final ProgramaRepository programaRepository;
  private final PeriodoAcademicoRepository periodoRepository;
  private final CalificacionRepository calificacionRepository;
  private final CompromisoRepository compromisoRepository;
  private final CalificacionService calificacionService;
  private final PromedioService promedioService;
  private final PasswordEncoder passwordEncoder;
  private final String contrasenaInicial;
  private final TransactionTemplate transaccion;
  private final ImportacionRepository importacionRepository;
  private final ComentarioEstudianteRepository comentarioRepository;
  private final FranjaHorariaRepository franjaRepository;
  private final CargadorClasesCampus cargadorClasesCampus;
  private final SemillaHeteroevaluacion semillaHeteroevaluacion;
  private final SemillaComentarios semillaComentarios;
  private final SemillaFormularios semillaFormularios;

  public CargadorDatosDesarrollo(
      FranjaHorariaRepository franjaRepository,
      ImportacionRepository importacionRepository,
      ComentarioEstudianteRepository comentarioRepository,
      UsuarioRepository usuarioRepository,
      FacultadRepository facultadRepository,
      ProgramaRepository programaRepository,
      PeriodoAcademicoRepository periodoRepository,
      CalificacionRepository calificacionRepository,
      CompromisoRepository compromisoRepository,
      CalificacionService calificacionService,
      PromedioService promedioService,
      PasswordEncoder passwordEncoder,
      @Value("${docentia.desarrollo.contrasena-inicial}") String contrasenaInicial,
      TransactionTemplate transaccion,
      CargadorClasesCampus cargadorClasesCampus,
      SemillaHeteroevaluacion semillaHeteroevaluacion,
      SemillaComentarios semillaComentarios,
      SemillaFormularios semillaFormularios) {
    this.semillaFormularios = semillaFormularios;
    this.semillaHeteroevaluacion = semillaHeteroevaluacion;
    this.semillaComentarios = semillaComentarios;
    this.transaccion = transaccion;
    this.importacionRepository = importacionRepository;
    this.comentarioRepository = comentarioRepository;
    this.franjaRepository = franjaRepository;
    this.usuarioRepository = usuarioRepository;
    this.facultadRepository = facultadRepository;
    this.programaRepository = programaRepository;
    this.periodoRepository = periodoRepository;
    this.calificacionRepository = calificacionRepository;
    this.compromisoRepository = compromisoRepository;
    this.calificacionService = calificacionService;
    this.promedioService = promedioService;
    this.passwordEncoder = passwordEncoder;
    this.contrasenaInicial = contrasenaInicial;
    this.cargadorClasesCampus = cargadorClasesCampus;
  }

  /**
   * El seed no es un suceso real: nada de lo que crea avisa a nadie. El silencio envuelve la
   * transacción porque los avisos salen después del commit.
   */
  @Override
  public void run(String... args) {
    SilencioNotificaciones.activar();
    try {
      transaccion.executeWithoutResult(estado -> cargar());
    } finally {
      SilencioNotificaciones.desactivar();
    }
  }

  private void cargar() {
    String hash = passwordEncoder.encode(contrasenaInicial);
    if (usuarioRepository.count() == 0) {
      sembrarCatalogoLocal(hash);
    }
    // El recorte Campus es idempotente: tiene que correr también si la base ya tenía el seed
    // de Ingeniería/Derecho (si no, Karen no existe y el login responde 401).
    periodoRepository
        .findByEsActivoTrue()
        .ifPresent(periodo -> cargadorClasesCampus.cargarDemo(periodo, hash));
    // Igual que Campus: corre también sobre una base que ya tenía el seed.
    semillaHeteroevaluacion.cargarSiFalta(hash);
    // Comentarios extra para ensayar el análisis; también sobre una base que ya tenía el seed.
    semillaComentarios.cargarSiFalta();
  }

  private void sembrarCatalogoLocal(String hash) {

    Facultad ingenieria = facultadRepository.save(new Facultad("Facultad de Ingeniería"));
    Facultad derecho =
        facultadRepository.save(new Facultad("Facultad de Derecho, Ciencias Sociales y Políticas"));
    Programa software = programaRepository.save(new Programa("Ingeniería de Software", ingenieria));
    Programa electronica =
        programaRepository.save(new Programa("Ingeniería Electrónica", ingenieria));
    Programa programaDerecho = programaRepository.save(new Programa("Derecho", derecho));

    Usuario vicerrector =
        usuarioRepository.save(
            Usuario.crearVicerrector("Elena Muñoz Cerón", "vicerrectoria" + DOMINIO_CORREO, hash));
    Usuario coordinadorIngenieria =
        usuarioRepository.save(
            Usuario.crearCoordinador(
                "Andrés Felipe Ordóñez",
                "coordinacion.ingenieria" + DOMINIO_CORREO,
                hash,
                ingenieria));
    Usuario coordinadorDerecho =
        usuarioRepository.save(
            Usuario.crearCoordinador(
                "Paola Andrea Sarria", "coordinacion.derecho" + DOMINIO_CORREO, hash, derecho));

    Usuario julian = crearDocente("Julián Camilo Hoyos", "julian.hoyos", hash, software);
    Usuario diana = crearDocente("Diana Marcela Astaiza", "diana.astaiza", hash, software);
    crearDocente("Óscar Iván Chantre", "oscar.chantre", hash, electronica);
    Usuario nora = crearDocente("Nora Patricia Muñoz", "nora.munoz", hash, software);
    Usuario camilo = crearDocente("Camilo Andrés Vidal", "camilo.vidal", hash, software);
    Usuario ramiro = crearDocente("Ramiro Antonio Paz", "ramiro.paz", hash, electronica);
    crearDocente("Luisa Fernanda Mosquera", "luisa.mosquera", hash, programaDerecho);
    crearDocente("Héctor Fabio Quilindo", "hector.quilindo", hash, programaDerecho);

    PeriodoAcademico periodoAnterior =
        periodoRepository.save(
            new PeriodoAcademico("2026-1", LocalDate.of(2026, 2, 2), LocalDate.of(2026, 6, 6)));
    PeriodoAcademico periodoActivo = crearPeriodoActivo();

    // El periodo anterior queda con las tres fuentes para que el histórico tenga un promedio
    // completo (Julián: 4.04 con los pesos 50/30/20).
    calificar(julian, periodoAnterior, FuenteEvaluacion.ESTUDIANTES, null, "4.2");
    calificar(julian, periodoAnterior, FuenteEvaluacion.COORDINACION, coordinadorIngenieria, "3.8");
    calificar(julian, periodoAnterior, FuenteEvaluacion.AUTOEVALUACION, julian, "4.0");

    // En el activo, Julián y Diana tienen las tres fuentes y su promedio sale completo (4.34 y
    // 4.31); Óscar, Luisa y Héctor siguen sin calificar para mostrar el caso incompleto. La nota de
    // estudiantes entra por el mismo camino que usará la importación desde Campus.
    calificar(julian, periodoActivo, FuenteEvaluacion.COORDINACION, coordinadorIngenieria, "4.3");
    calificar(julian, periodoActivo, FuenteEvaluacion.AUTOEVALUACION, julian, "4.0");
    calificarEstudiantes(julian, periodoActivo, "4.5");
    calificar(diana, periodoActivo, FuenteEvaluacion.COORDINACION, coordinadorIngenieria, "4.6");
    calificar(diana, periodoActivo, FuenteEvaluacion.AUTOEVALUACION, diana, "4.4");
    calificarEstudiantes(diana, periodoActivo, "4.1");

    // Un docente en cada extremo de la escala de la Resolución 074 para mostrar categorías y
    // acciones: Nora con 2.85 (bajo, plan de mejoramiento), Camilo 4.75 y Ramiro 1.90.
    calificarTresFuentes(nora, periodoActivo, coordinadorIngenieria, "2.8", "2.9", "2.9");
    calificarTresFuentes(camilo, periodoActivo, coordinadorIngenieria, "4.8", "4.7", "4.7");
    calificarTresFuentes(ramiro, periodoActivo, coordinadorIngenieria, "1.9", "1.9", "1.9");

    // Comentarios de estudiantes como si vinieran del CSV de Power BI. Julián tiene 13 encuestas
    // cerradas (alcanza para el análisis con IA) y dos de sus comentarios son ofensivos o sobre su
    // persona, para que el filtro los excluya. Diana tiene 6: su análisis sale INSUFICIENTE.
    cargarComentarios(
        coordinadorIngenieria,
        periodoActivo,
        julian,
        List.of(
            "Explica con ejemplos reales del sector y se nota que domina la arquitectura de"
                + " software.",
            "Las notas de los parciales se demoran más de dos semanas en publicarse.",
            "Responde las dudas en clase con paciencia.",
            "A veces el ritmo de la clase es muy rápido y cuesta seguirlo.",
            "Los talleres prácticos ayudan mucho a entender los patrones de diseño.",
            "La metodología es pésima: lee las diapositivas y no explica nada.",
            "Es puntual y siempre llega preparado.",
            "Los criterios de evaluación de los proyectos no son claros desde el inicio.",
            "Recomiendo su materia, se aprende bastante.",
            "La retroalimentación de las entregas llega tarde.",
            "Excelente manejo del repositorio y de las herramientas del curso.",
            "Es un profesor muy feo y se viste mal.",
            "Ese profesor es un idiota."));
    cargarComentarios(
        coordinadorIngenieria,
        periodoActivo,
        diana,
        List.of(
            "Muy clara en sus explicaciones.",
            "Las clases empiezan siempre a tiempo.",
            "Deja suficiente material de estudio.",
            "Califica con criterios justos.",
            "Podría dejar más ejercicios para practicar.",
            "Buena docente."));

    // Franjas abiertas para las citas de retroalimentación: los docentes de Ingeniería ven las del
    // coordinador de Ingeniería y los de Derecho las de su coordinadora.
    for (String hora : List.of("08:00", "08:30", "09:00", "09:30")) {
      franjaRepository.save(
          new FranjaHoraria(
              coordinadorIngenieria,
              periodoActivo,
              LocalDate.of(2026, 11, 9),
              LocalTime.parse(hora)));
    }
    for (String hora : List.of("14:00", "14:30")) {
      franjaRepository.save(
          new FranjaHoraria(
              coordinadorIngenieria,
              periodoActivo,
              LocalDate.of(2026, 11, 10),
              LocalTime.parse(hora)));
    }
    franjaRepository.save(
        new FranjaHoraria(
            coordinadorDerecho, periodoActivo, LocalDate.of(2026, 11, 9), LocalTime.of(10, 0)));

    compromisoRepository.save(
        new Compromiso(
            julian,
            periodoAnterior,
            "Publicar las notas parciales dentro de los ocho días siguientes a cada corte",
            LocalDate.of(2026, 11, 13)));
    compromisoRepository.save(
        new Compromiso(
            julian,
            periodoAnterior,
            "Actualizar el microcurrículo de Arquitectura de Software",
            LocalDate.of(2026, 12, 1)));
    compromisoRepository.save(
        new Compromiso(
            julian,
            periodoAnterior,
            "Entregar el plan de asignatura firmado por el programa",
            LocalDate.of(2026, 9, 30)));
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

  private Usuario crearDocente(
      String nombre, String usuarioCorreo, String hash, Programa programa) {
    return usuarioRepository.save(
        Usuario.crearDocente(nombre, usuarioCorreo + DOMINIO_CORREO, hash, programa));
  }

  private PeriodoAcademico crearPeriodoActivo() {
    PeriodoAcademico periodo =
        new PeriodoAcademico("2026-2", LocalDate.of(2026, 8, 3), LocalDate.of(2026, 12, 5));
    // La recolección cubre las semanas de desarrollo y la entrega del 6 de noviembre, para que en
    // local se pueda calificar y cargar el CSV sin tocar fechas.
    periodo.programarEtapa(
        TipoEtapa.RECOLECCION_CALIFICACIONES,
        LocalDate.of(2026, 9, 28),
        LocalDate.of(2026, 11, 13));
    // En desarrollo las etapas se solapan para poder demostrar todo el flujo entre la demo del
    // sprint y la entrega de noviembre; en producción las define la vicerrectoría.
    periodo.programarEtapa(
        TipoEtapa.REVISION_HALLAZGOS, LocalDate.of(2026, 10, 13), LocalDate.of(2026, 11, 20));
    periodo.programarEtapa(
        TipoEtapa.AGENDA_CITAS, LocalDate.of(2026, 10, 13), LocalDate.of(2026, 11, 27));
    periodo.programarEtapa(
        TipoEtapa.FIRMA_ACTAS, LocalDate.of(2026, 10, 13), LocalDate.of(2026, 12, 4));
    periodo.setEsActivo(true);
    return periodoRepository.save(periodo);
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
    calificacion.registrar(notasIguales(nota), null, evaluador);
    calificacionRepository.saveAndFlush(calificacion);
    promedioService.recalcular(docente.getIdUsuario(), periodo.getIdPeriodo());
  }

  private void calificarTresFuentes(
      Usuario docente,
      PeriodoAcademico periodo,
      Usuario coordinador,
      String estudiantes,
      String coordinacion,
      String autoevaluacion) {
    calificar(docente, periodo, FuenteEvaluacion.COORDINACION, coordinador, coordinacion);
    calificar(docente, periodo, FuenteEvaluacion.AUTOEVALUACION, docente, autoevaluacion);
    calificarEstudiantes(docente, periodo, estudiantes);
  }

  private void calificarEstudiantes(Usuario docente, PeriodoAcademico periodo, String nota) {
    calificacionService.registrarCalificacionEstudiantes(docente, periodo, notasIguales(nota));
  }

  private NotasAspectos notasIguales(String nota) {
    BigDecimal valor = new BigDecimal(nota);
    return new NotasAspectos(valor, valor, valor, valor, valor);
  }
}
