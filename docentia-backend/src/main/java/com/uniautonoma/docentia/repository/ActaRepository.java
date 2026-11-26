package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Acta;
import com.uniautonoma.docentia.entity.EstadoActa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

  /** Actas firmadas (vigentes o reemplazadas), de la firma más reciente a la más antigua. */
  @EntityGraph(attributePaths = {"docente", "periodo"})
  List<Acta> findByCodigoVerificacionIsNotNullOrderByFechaFirmaDesc();

  /** Respuestas del formulario de estudiantes que tiene el docente en el periodo. */
  @Query(
      value =
          "select count(*) from respuestas_heteroevaluacion where docente_id = ?1 and periodo_id = ?2",
      nativeQuery = true)
  long contarRespuestasEstudiantes(Long idDocente, Long idPeriodo);
}
