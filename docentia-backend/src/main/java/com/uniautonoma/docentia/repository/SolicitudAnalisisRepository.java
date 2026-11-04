package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.EstadoSolicitudAnalisis;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

  /** Última revisión publicada de un docente en un periodo. */
  Optional<SolicitudAnalisis>
      findFirstByDocenteIdUsuarioAndPeriodoIdPeriodoAndEstadoBorradorOrderByIdSolicitudDesc(
          Long idDocente, Long idPeriodo, String estadoBorrador);

  /** Toma la solicitud con bloqueo de fila: dos publicaciones simultáneas van en fila. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from SolicitudAnalisis s where s.idSolicitud = :idSolicitud")
  Optional<SolicitudAnalisis> bloquear(Long idSolicitud);

  /** Una solicitud que tumbó el proceso varias veces no vuelve a la cola. */
  @Modifying
  @Query(
      "update SolicitudAnalisis s set s.estado ="
          + " com.uniautonoma.docentia.entity.EstadoSolicitudAnalisis.ERROR, s.mensaje = :mensaje"
          + " where s.estado = com.uniautonoma.docentia.entity.EstadoSolicitudAnalisis.EN_PROCESO"
          + " and s.intentos >= :maximo")
  int marcarErrorHuerfanas(int maximo, String mensaje);

  long countByEstado(EstadoSolicitudAnalisis estado);

  /** Solicitudes del periodo de varios docentes, de la más nueva a la más antigua. */
  @Query(
      """
      select s from SolicitudAnalisis s
      where s.periodo.idPeriodo = :idPeriodo and s.docente.idUsuario in :idsDocentes
      order by s.idSolicitud desc
      """)
  List<SolicitudAnalisis> buscarDePeriodo(Long idPeriodo, Collection<Long> idsDocentes);
}
