package com.uniautonoma.docentia.service.campus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniautonoma.docentia.entity.Clase;
import com.uniautonoma.docentia.entity.Curso;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Matricula;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.ClaseRepository;
import com.uniautonoma.docentia.repository.CursoRepository;
import com.uniautonoma.docentia.repository.FacultadRepository;
import com.uniautonoma.docentia.repository.MatriculaRepository;
import com.uniautonoma.docentia.repository.ProgramaRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.service.campus.CampusSeed.CampusClase;
import com.uniautonoma.docentia.service.campus.CampusSeed.CampusDocenteClase;
import com.uniautonoma.docentia.service.campus.CampusSeed.CampusFacultad;
import com.uniautonoma.docentia.service.campus.CampusSeed.CampusPersona;
import com.uniautonoma.docentia.service.campus.CampusSeed.CampusPrograma;
import com.uniautonoma.docentia.service.campus.CampusSeed.CampusRol;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Materializa el JSON de Campus (seed o contrato) en facultades, programas, clases y matrículas. Es
 * idempotente: si el correo o el código ya existen, reutiliza el registro.
 */
@Component
public class CargadorClasesCampus {

  public static final String RECURSO_DEMO = "seed/campus-clases-demo.json";

  private final ObjectMapper objectMapper;
  private final FacultadRepository facultadRepository;
  private final ProgramaRepository programaRepository;
  private final UsuarioRepository usuarioRepository;
  private final CursoRepository cursoRepository;
  private final ClaseRepository claseRepository;
  private final MatriculaRepository matriculaRepository;

  public CargadorClasesCampus(
      ObjectMapper objectMapper,
      FacultadRepository facultadRepository,
      ProgramaRepository programaRepository,
      UsuarioRepository usuarioRepository,
      CursoRepository cursoRepository,
      ClaseRepository claseRepository,
      MatriculaRepository matriculaRepository) {
    this.objectMapper = objectMapper;
    this.facultadRepository = facultadRepository;
    this.programaRepository = programaRepository;
    this.usuarioRepository = usuarioRepository;
    this.cursoRepository = cursoRepository;
    this.claseRepository = claseRepository;
    this.matriculaRepository = matriculaRepository;
  }

  /** Carga el seed de classpath sobre el periodo indicado. */
  @Transactional
  public CampusSeed cargarDemo(PeriodoAcademico periodo, String hashContrasena) {
    try (InputStream entrada = new ClassPathResource(RECURSO_DEMO).getInputStream()) {
      CampusSeed seed = objectMapper.readValue(entrada, CampusSeed.class);
      cargar(seed, periodo, hashContrasena);
      return seed;
    } catch (IOException fallo) {
      throw new UncheckedIOException("No se pudo leer " + RECURSO_DEMO, fallo);
    }
  }

  @Transactional
  public void cargar(CampusSeed seed, PeriodoAcademico periodo, String hashContrasena) {
    if (seed.facultades() != null) {
      seed.facultades().forEach(facultad -> persistirFacultad(facultad, hashContrasena));
    }
    if (seed.personas() != null) {
      seed.personas().forEach(persona -> persistirPersona(persona, hashContrasena));
      seed.personas().forEach(persona -> persistirClases(persona, periodo));
    }
  }

  private void persistirFacultad(CampusFacultad entrada, String hash) {
    Facultad facultad =
        facultadRepository
            .findByCodigoIgnoreCaseAndEsActivaTrue(entrada.codFacultad())
            .orElseGet(
                () ->
                    facultadRepository.save(
                        new Facultad(entrada.facultad(), entrada.codFacultad())));
    if (entrada.programas() != null) {
      for (CampusPrograma programa : entrada.programas()) {
        persistirPrograma(facultad, programa);
      }
    }
    if (entrada.coordinador() != null && entrada.coordinador().correo() != null) {
      usuarioRepository
          .findByCorreoIgnoreCase(entrada.coordinador().correo())
          .orElseGet(
              () ->
                  usuarioRepository.save(
                      Usuario.crearCoordinador(
                          entrada.coordinador().nombre(),
                          entrada.coordinador().correo(),
                          hash,
                          facultad)));
    }
  }

  private Programa persistirPrograma(Facultad facultad, CampusPrograma entrada) {
    return programaRepository
        .findByCodigoIgnoreCaseAndEsActivoTrue(entrada.codPrograma())
        .orElseGet(
            () ->
                programaRepository.save(
                    new Programa(entrada.programa(), entrada.codPrograma(), facultad)));
  }

  private void persistirPersona(CampusPersona persona, String hash) {
    if (persona.correo() == null || persona.roles() == null || persona.roles().isEmpty()) {
      return;
    }
    if (usuarioRepository.findByCorreoIgnoreCase(persona.correo()).isPresent()) {
      return;
    }
    Programa programa =
        programaRepository
            .findByCodigoIgnoreCaseAndEsActivoTrue(persona.programa())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Programa " + persona.programa() + " no está en el seed de facultades"));
    CampusRol rolCampus = persona.roles().getFirst();
    String nombre = (persona.nombres() + " " + persona.apellidos()).trim();
    Usuario creado;
    if ("Estudiante".equalsIgnoreCase(rolCampus.rol())) {
      creado = Usuario.crearEstudiante(nombre, persona.correo(), hash, programa);
    } else if ("Docente".equalsIgnoreCase(rolCampus.rol())) {
      creado = Usuario.crearDocente(nombre, persona.correo(), hash, programa);
    } else {
      return;
    }
    creado.setCodigo(rolCampus.codigo());
    usuarioRepository.save(creado);
  }

  private void persistirClases(CampusPersona persona, PeriodoAcademico periodo) {
    Usuario evaluador = usuarioRepository.findByCorreoIgnoreCase(persona.correo()).orElse(null);
    if (evaluador == null || persona.roles() == null) {
      return;
    }
    Programa programa = evaluador.getPrograma();
    for (CampusRol rol : persona.roles()) {
      if (rol.clases() == null) {
        continue;
      }
      for (CampusClase claseCampus : rol.clases()) {
        Clase clase = persistirClase(claseCampus, periodo, programa);
        vincularDocentes(clase, claseCampus.docentes());
        if ("Matriculado".equalsIgnoreCase(claseCampus.relacion())
            && evaluador.getRol() == Rol.ESTUDIANTE) {
          matricular(clase, evaluador);
        }
      }
    }
  }

  private Clase persistirClase(CampusClase entrada, PeriodoAcademico periodo, Programa programa) {
    Curso curso =
        cursoRepository
            .findByCodigoIgnoreCase(entrada.codCurso())
            .orElseGet(
                () ->
                    cursoRepository.save(new Curso(entrada.codCurso(), entrada.curso(), programa)));
    Clase clase =
        claseRepository
            .findByPeriodoIdPeriodoAndCursoIdAndGrupoAndJornada(
                periodo.getIdPeriodo(), curso.getId(), entrada.grupo(), entrada.jornada())
            .orElseGet(
                () ->
                    claseRepository.save(
                        new Clase(
                            periodo,
                            curso,
                            entrada.grupo(),
                            entrada.jornada(),
                            entrada.tipoCurso() == null ? "S001" : entrada.tipoCurso())));
    if (entrada.numEstudiantes() != null) {
      clase.setInscritosEsperados(entrada.numEstudiantes());
    }
    return claseRepository.save(clase);
  }

  private void vincularDocentes(Clase clase, List<CampusDocenteClase> docentes) {
    if (docentes == null) {
      return;
    }
    for (CampusDocenteClase docenteCampus : docentes) {
      usuarioRepository
          .findByCorreoIgnoreCase(docenteCampus.correo())
          .filter(usuario -> usuario.getRol() == Rol.DOCENTE)
          .ifPresent(clase::agregarDocente);
    }
    claseRepository.save(clase);
  }

  private void matricular(Clase clase, Usuario estudiante) {
    matriculaRepository
        .findByClaseIdAndEstudianteIdUsuarioAndEsActivaTrue(
            clase.getId(), estudiante.getIdUsuario())
        .orElseGet(() -> matriculaRepository.save(new Matricula(clase, estudiante)));
  }
}
