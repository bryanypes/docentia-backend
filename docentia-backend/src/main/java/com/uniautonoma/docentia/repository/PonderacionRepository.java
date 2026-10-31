package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Ponderacion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de las ponderaciones configuradas por periodo. */
public interface PonderacionRepository extends JpaRepository<Ponderacion, Long> {

  /**
   * Busca la ponderación configurada para un periodo.
   *
   * @param idPeriodo identificador del periodo
   * @return la ponderación, o vacío si el periodo usa los pesos predeterminados
   */
  Optional<Ponderacion> findByPeriodoIdPeriodo(Long idPeriodo);
}
