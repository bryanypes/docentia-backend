package com.uniautonoma.docentia.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.security.UsuarioSesion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class UsuarioRepositoryTest extends PruebaIntegracion {

  private static final String HASH = "hash-de-prueba";

  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private FacultadRepository facultadRepository;
  @Autowired private ProgramaRepository programaRepository;

  private Facultad ingenieria;
  private Programa ingenieriaSoftware;

  @BeforeEach
  void crearFacultadYPrograma() {
    ingenieria = facultadRepository.save(new Facultad("Ingeniería"));
    ingenieriaSoftware =
        programaRepository.save(new Programa("Ingeniería de Software", ingenieria));
  }

  @Test
  void laSesionDelCoordinadorLlevaSuFacultad() {
    Usuario coordinador =
        usuarioRepository.saveAndFlush(
            Usuario.crearCoordinador("Marta Ruiz", "marta@uniautonoma.edu.co", HASH, ingenieria));

    UsuarioSesion sesion = UsuarioSesion.desde(coordinador);

    assertThat(sesion.esCoordinador()).isTrue();
    assertThat(sesion.idFacultad()).isEqualTo(ingenieria.getIdFacultad());
    assertThat(sesion.idPrograma()).isNull();
  }

  @Test
  void laSesionDelDocenteHeredaLaFacultadDeSuPrograma() {
    Usuario docente =
        usuarioRepository.saveAndFlush(
            Usuario.crearDocente("Luis Paz", "luis@uniautonoma.edu.co", HASH, ingenieriaSoftware));

    UsuarioSesion sesion = UsuarioSesion.desde(docente);

    assertThat(sesion.idPrograma()).isEqualTo(ingenieriaSoftware.getIdPrograma());
    assertThat(sesion.idFacultad()).isEqualTo(ingenieria.getIdFacultad());
  }

  @Test
  void laSesionDelVicerrectorNoTieneAlcanceRestringido() {
    Usuario vicerrector =
        usuarioRepository.saveAndFlush(
            Usuario.crearVicerrector("Ana Mera", "ana@uniautonoma.edu.co", HASH));

    UsuarioSesion sesion = UsuarioSesion.desde(vicerrector);

    assertThat(sesion.esVicerrector()).isTrue();
    assertThat(sesion.idFacultad()).isNull();
    assertThat(sesion.idPrograma()).isNull();
  }

  @Test
  void rechazaUnCoordinadorSinFacultad() {
    Usuario coordinador =
        Usuario.crearCoordinador("Sin Facultad", "sinfacultad@uniautonoma.edu.co", HASH, null);

    assertThatThrownBy(() -> usuarioRepository.saveAndFlush(coordinador))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rechazaUnCorreoRepetidoAunqueCambienLasMayusculas() {
    usuarioRepository.saveAndFlush(
        Usuario.crearVicerrector("Ana Mera", "ana@uniautonoma.edu.co", HASH));
    Usuario repetido = Usuario.crearVicerrector("Otra Ana", "ANA@uniautonoma.edu.co", HASH);

    assertThatThrownBy(() -> usuarioRepository.saveAndFlush(repetido))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void encuentraAlUsuarioActivoPorCorreoSinDistinguirMayusculas() {
    usuarioRepository.saveAndFlush(
        Usuario.crearVicerrector("Ana Mera", "ana@uniautonoma.edu.co", HASH));

    assertThat(usuarioRepository.findByCorreoIgnoreCaseAndEsActivoTrue("Ana@UniAutonoma.edu.co"))
        .isPresent();
  }
}
