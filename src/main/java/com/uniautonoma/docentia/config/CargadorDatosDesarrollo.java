package com.uniautonoma.docentia.config;

import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.NotasAspectos;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.repository.CompromisoRepository;
import com.uniautonoma.docentia.repository.FacultadRepository;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.ProgramaRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.service.CalificacionService;
import com.uniautonoma.docentia.service.PromedioService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

  public CargadorDatosDesarrollo(
      UsuarioRepository usuarioRepository,
      FacultadRepository facultadRepository,
      ProgramaRepository programaRepository,
      PeriodoAcademicoRepository periodoRepository,
      CalificacionRepository calificacionRepository,
      CompromisoRepository compromisoRepository,
      CalificacionService calificacionService,
      PromedioService promedioService,
      PasswordEncoder passwordEncoder,
      @Value("${docentia.desarrollo.contrasena-inicial}") String contrasenaInicial) {
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
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (usuarioRepository.count() > 0) {
      return;
    }
    String hash = passwordEncoder.encode(contrasenaInicial);

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
    usuarioRepository.save(
        Usuario.crearCoordinador(
            "Paola Andrea Sarria", "coordinacion.derecho" + DOMINIO_CORREO, hash, derecho));

    Usuario julian = crearDocente("Julián Camilo Hoyos", "julian.hoyos", hash, software);
    Usuario diana = crearDocente("Diana Marcela Astaiza", "diana.astaiza", hash, software);
    crearDocente("Óscar Iván Chantre", "oscar.chantre", hash, electronica);
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
    calificar(julian, periodoAnterior, FuenteEvaluacion.VICERRECTORIA, vicerrector, "4.0");

    // En el activo, Julián y Diana tienen las tres fuentes y su promedio sale completo (4.34 y
    // 4.31); Óscar, Luisa y Héctor siguen sin calificar para mostrar el caso incompleto. La nota de
    // estudiantes entra por el mismo camino que usará la importación desde Campus.
    calificar(julian, periodoActivo, FuenteEvaluacion.COORDINACION, coordinadorIngenieria, "4.3");
    calificar(julian, periodoActivo, FuenteEvaluacion.VICERRECTORIA, vicerrector, "4.0");
    calificarEstudiantes(julian, periodoActivo, "4.5");
    calificar(diana, periodoActivo, FuenteEvaluacion.COORDINACION, coordinadorIngenieria, "4.6");
    calificar(diana, periodoActivo, FuenteEvaluacion.VICERRECTORIA, vicerrector, "4.4");
    calificarEstudiantes(diana, periodoActivo, "4.1");

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
  }

  private Usuario crearDocente(
      String nombre, String usuarioCorreo, String hash, Programa programa) {
    return usuarioRepository.save(
        Usuario.crearDocente(nombre, usuarioCorreo + DOMINIO_CORREO, hash, programa));
  }

  private PeriodoAcademico crearPeriodoActivo() {
    PeriodoAcademico periodo =
        new PeriodoAcademico("2026-2", LocalDate.of(2026, 8, 3), LocalDate.of(2026, 12, 5));
    periodo.programarEtapa(
        TipoEtapa.EVALUACION_ESTUDIANTES, LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 30));
    periodo.programarEtapa(
        TipoEtapa.CALIFICACION_COORDINACION, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 13));
    periodo.programarEtapa(
        TipoEtapa.CALIFICACION_VICERRECTORIA,
        LocalDate.of(2026, 11, 9),
        LocalDate.of(2026, 11, 20));
    periodo.programarEtapa(
        TipoEtapa.RETROALIMENTACION, LocalDate.of(2026, 11, 23), LocalDate.of(2026, 12, 4));
    periodo.setEsActivo(true);
    return periodoRepository.save(periodo);
  }

  private void calificar(
      Usuario docente,
      PeriodoAcademico periodo,
      FuenteEvaluacion fuente,
      Usuario evaluador,
      String nota) {
    Calificacion calificacion = new Calificacion(docente, periodo, fuente);
    calificacion.registrar(notasIguales(nota), null, evaluador);
    calificacionRepository.saveAndFlush(calificacion);
    promedioService.recalcular(docente.getIdUsuario(), periodo.getIdPeriodo());
  }

  private void calificarEstudiantes(Usuario docente, PeriodoAcademico periodo, String nota) {
    calificacionService.registrarCalificacionEstudiantes(docente, periodo, notasIguales(nota));
  }

  private NotasAspectos notasIguales(String nota) {
    BigDecimal valor = new BigDecimal(nota);
    return new NotasAspectos(valor, valor, valor, valor, valor);
  }
}
