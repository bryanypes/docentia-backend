package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

  /**
   * Lista los periodos en los que un docente tiene al menos una calificación.
   *
   * @param idDocente identificador del docente
   * @return periodos del más reciente al más antiguo
   */
  @Query(
      """
      select distinct c.periodo from Calificacion c
      where c.docente.idUsuario = :idDocente
      order by c.periodo.fechaInicio desc
      """)
  List<PeriodoAcademico> buscarPeriodosCalificados(Long idDocente);

  /**
   * Lista los docentes que tienen al menos una calificación en un periodo.
   *
   * @param idPeriodo identificador del periodo
   * @return identificadores de los docentes, sin repetir
   */
  @Query(
      """
      select distinct c.docente.idUsuario from Calificacion c
      where c.periodo.idPeriodo = :idPeriodo
      """)
  List<Long> buscarDocentesCalificados(Long idPeriodo);

  /**
   * Lista los pares docente y periodo que tienen calificaciones pero todavía no un promedio
   * guardado, como en las bases creadas antes de que existiera la tabla de promedios.
   *
   * @return pares sin promedio, sin repetir
   */
  @Query(
      """
      select distinct new com.uniautonoma.docentia.repository.DocentePeriodo(
          c.docente.idUsuario, c.periodo.idPeriodo)
      from Calificacion c
      where not exists (
          select 1 from PromedioDocente p
          where p.docente = c.docente and p.periodo = c.periodo)
      """)
  List<DocentePeriodo> buscarSinPromedio();
}
