package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Acta;
import com.uniautonoma.docentia.entity.EstadoActa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a las actas de retroalimentación. */
public interface ActaRepository extends JpaRepository<Acta, Long> {

  /** Todas las versiones del acta de un docente, de la más reciente a la más antigua. */
  @EntityGraph(attributePaths = {"docente", "periodo"})
  List<Acta> findByDocenteIdUsuarioOrderByPeriodoFechaInicioDescVersionDesc(Long idDocente);

  Optional<Acta> findFirstByDocenteIdUsuarioAndPeriodoIdPeriodoOrderByVersionDesc(
      Long idDocente, Long idPeriodo);

  boolean existsByDocenteIdUsuarioAndPeriodoIdPeriodoAndEstadoNot(
      Long idDocente, Long idPeriodo, EstadoActa estado);

  @EntityGraph(attributePaths = {"docente", "periodo"})
  Optional<Acta> findByCodigoVerificacion(String codigoVerificacion);
}
