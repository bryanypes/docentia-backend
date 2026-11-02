package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Matricula;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Inscripciones activas de un estudiante a una clase. */
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

  Optional<Matricula> findByClaseIdAndEstudianteIdUsuarioAndEsActivaTrue(
      Long idClase, Long idEstudiante);

  @EntityGraph(
      attributePaths = {
        "clase",
        "clase.curso",
        "clase.curso.programa",
        "clase.docentes",
        "clase.periodo"
      })
  List<Matricula> findByEstudianteIdUsuarioAndEsActivaTrue(Long idEstudiante);

  @EntityGraph(
      attributePaths = {
        "clase",
        "clase.curso",
        "clase.curso.programa",
        "clase.docentes",
        "clase.periodo"
      })
  Optional<Matricula> findConClaseByClaseIdAndEstudianteIdUsuarioAndEsActivaTrue(
      Long idClase, Long idEstudiante);

  long countByClaseIdAndEsActivaTrue(Long idClase);
}
