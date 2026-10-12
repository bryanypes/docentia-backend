package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.PromedioDocente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de los promedios ponderados guardados. */
public interface PromedioDocenteRepository extends JpaRepository<PromedioDocente, Long> {

  /**
   * Busca el promedio guardado de un docente en un periodo.
   *
   * @param idDocente identificador del docente
   * @param idPeriodo identificador del periodo
   * @return el promedio, o vacío si el docente no tiene calificaciones en ese periodo
   */
  Optional<PromedioDocente> findByDocenteIdUsuarioAndPeriodoIdPeriodo(
      Long idDocente, Long idPeriodo);
}
