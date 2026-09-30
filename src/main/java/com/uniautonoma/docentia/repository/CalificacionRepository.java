package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de las calificaciones por fuente. */
public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {

  /**
   * Busca la calificación que una fuente le dio a un docente en un periodo.
   *
   * @param idDocente identificador del docente
   * @param idPeriodo identificador del periodo
   * @param fuente fuente que califica
   * @return la calificación, o vacío si esa fuente aún no ha calificado
   */
  Optional<Calificacion> findByDocenteIdUsuarioAndPeriodoIdPeriodoAndFuente(
      Long idDocente, Long idPeriodo, FuenteEvaluacion fuente);

  /**
   * Lista las calificaciones que un docente recibió en un periodo, una por fuente como máximo.
   *
   * @param idDocente identificador del docente
   * @param idPeriodo identificador del periodo
   * @return calificaciones registradas hasta el momento
   */
  List<Calificacion> findByDocenteIdUsuarioAndPeriodoIdPeriodo(Long idDocente, Long idPeriodo);
}
