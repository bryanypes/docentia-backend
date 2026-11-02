package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Clase;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Ofertas de curso en un periodo (grupo y jornada). */
public interface ClaseRepository extends JpaRepository<Clase, Long> {

  Optional<Clase> findByPeriodoIdPeriodoAndCursoIdAndGrupoAndJornada(
      Long idPeriodo, Long idCurso, String grupo, String jornada);

  @EntityGraph(attributePaths = {"curso", "docentes"})
  List<Clase> findByPeriodoIdPeriodoAndDocentesIdUsuario(Long idPeriodo, Long idDocente);
}
