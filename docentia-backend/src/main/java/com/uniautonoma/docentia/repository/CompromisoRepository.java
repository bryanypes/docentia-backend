package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.EstadoCompromiso;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Acceso a datos de los compromisos de mejora. */
public interface CompromisoRepository extends JpaRepository<Compromiso, Long> {

  /**
   * Lista los compromisos de un docente en un estado, empezando por el que vence primero. Los de un
   * acta sin firmar o anulada todavía no existen para el docente.
   *
   * @param idDocente identificador del docente
   * @param estado estado buscado
   * @return compromisos con su periodo cargado
   */
  @EntityGraph(attributePaths = "periodo")
  @Query(
      """
      select c from Compromiso c left join c.acta a
      where c.docente.idUsuario = :idDocente and c.estado = :estado
        and (a is null or a.estado = com.uniautonoma.docentia.entity.EstadoActa.FIRMADA)
      order by c.fechaLimite
      """)
  List<Compromiso> findByDocenteIdUsuarioAndEstadoOrderByFechaLimiteAsc(
      Long idDocente, EstadoCompromiso estado);

  /** Compromisos en firme de un docente en un periodo, empezando por el que vence primero. */
  @EntityGraph(attributePaths = "periodo")
  @Query(
      """
      select c from Compromiso c left join c.acta a
      where c.docente.idUsuario = :idDocente and c.periodo.idPeriodo = :idPeriodo
        and (a is null or a.estado = com.uniautonoma.docentia.entity.EstadoActa.FIRMADA)
      order by c.fechaLimite
      """)
  List<Compromiso> buscarEnFirme(Long idDocente, Long idPeriodo);
}
