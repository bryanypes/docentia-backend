package com.uniautonoma.docentia.config;

import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.FacultadRepository;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.ProgramaRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
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
  private final PasswordEncoder passwordEncoder;
  private final String contrasenaInicial;

  public CargadorDatosDesarrollo(
      UsuarioRepository usuarioRepository,
      FacultadRepository facultadRepository,
      ProgramaRepository programaRepository,
      PeriodoAcademicoRepository periodoRepository,
      PasswordEncoder passwordEncoder,
      @Value("${docentia.desarrollo.contrasena-inicial}") String contrasenaInicial) {
    this.usuarioRepository = usuarioRepository;
    this.facultadRepository = facultadRepository;
    this.programaRepository = programaRepository;
    this.periodoRepository = periodoRepository;
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

    usuarioRepository.save(
        Usuario.crearVicerrector("Elena Muñoz Cerón", "vicerrectoria" + DOMINIO_CORREO, hash));
    usuarioRepository.save(
        Usuario.crearCoordinador(
            "Andrés Felipe Ordóñez", "coordinacion.ingenieria" + DOMINIO_CORREO, hash, ingenieria));
    usuarioRepository.save(
        Usuario.crearCoordinador(
            "Paola Andrea Sarria", "coordinacion.derecho" + DOMINIO_CORREO, hash, derecho));

    crearDocente("Julián Camilo Hoyos", "julian.hoyos", hash, software);
    crearDocente("Diana Marcela Astaiza", "diana.astaiza", hash, software);
    crearDocente("Óscar Iván Chantre", "oscar.chantre", hash, electronica);
    crearDocente("Luisa Fernanda Mosquera", "luisa.mosquera", hash, programaDerecho);
    crearDocente("Héctor Fabio Quilindo", "hector.quilindo", hash, programaDerecho);

    crearPeriodoActivo();
  }

  private void crearDocente(String nombre, String usuarioCorreo, String hash, Programa programa) {
    usuarioRepository.save(
        Usuario.crearDocente(nombre, usuarioCorreo + DOMINIO_CORREO, hash, programa));
  }

  private void crearPeriodoActivo() {
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
    periodoRepository.save(periodo);
  }
}
