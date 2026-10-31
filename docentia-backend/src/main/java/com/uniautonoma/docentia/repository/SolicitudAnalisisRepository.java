package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.EstadoSolicitudAnalisis;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Acceso a las solicitudes de análisis y su borrador. */
public interface SolicitudAnalisisRepository extends JpaRepository<SolicitudAnalisis, Long> {

  Optional<SolicitudAnalisis> findFirstByDocenteIdUsuarioAndPeriodoIdPeriodoAndEstadoIn(
      Long idDocente, Long idPeriodo, Collection<EstadoSolicitudAnalisis> estados);

  List<SolicitudAnalisis> findByDocenteIdUsuarioAndPeriodoIdPeriodoOrderByIdSolicitudDesc(
      Long idDocente, Long idPeriodo);

  /**
   * Devuelve a la cola las solicitudes que quedaron a medias, por ejemplo porque la aplicación se
   * detuvo mientras las procesaba.
   *
   * @return cuántas se devolvieron
   */
  @Modifying
  @Query(
      "update SolicitudAnalisis s set s.estado = com.uniautonoma.docentia.entity.EstadoSolicitudAnalisis.EN_COLA"
          + " where s.estado = com.uniautonoma.docentia.entity.EstadoSolicitudAnalisis.EN_PROCESO")
  int devolverEnProcesoACola();
}
