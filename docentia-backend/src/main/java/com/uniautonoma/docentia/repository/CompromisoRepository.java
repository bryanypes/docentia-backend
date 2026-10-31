package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.EstadoCompromiso;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de los compromisos de mejora. */
public interface CompromisoRepository extends JpaRepository<Compromiso, Long> {

  /**
   * Lista los compromisos de un docente en un estado, empezando por el que vence primero.
   *
   * @param idDocente identificador del docente
   * @param estado estado buscado
   * @return compromisos con su periodo cargado
   */
  @EntityGraph(attributePaths = "periodo")
  List<Compromiso> findByDocenteIdUsuarioAndEstadoOrderByFechaLimiteAsc(
      Long idDocente, EstadoCompromiso estado);
}
