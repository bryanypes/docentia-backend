package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Matricula;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

  /**
   * Cuenta si hay matrículas activas en un período (a través de clase.periodo).
   *
   * @param idPeriodo identificador del periodo académico
   * @return cantidad de matrículas activas del periodo
   */
  @Query("SELECT COUNT(m) FROM Matricula m WHERE m.clase.periodo.id = :idPeriodo AND m.esActiva = true")
  long countByPeriodoIdAndEsActivaTrue(Long idPeriodo);
}
