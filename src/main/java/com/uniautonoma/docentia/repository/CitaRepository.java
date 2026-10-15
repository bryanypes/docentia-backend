package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.EstadoCita;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** Acceso a las citas reservadas en las franjas. */
public interface CitaRepository extends JpaRepository<Cita, Long> {

  boolean existsByFranjaIdFranjaAndEstado(Long idFranja, EstadoCita estado);

  boolean existsByFranjaIdFranja(Long idFranja);

  Optional<Cita> findByFranjaIdFranjaAndEstado(Long idFranja, EstadoCita estado);

  /** Cita activa del docente en el periodo, bloqueada para reemplazarla en la misma transacción. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select c from Cita c
      where c.docente.idUsuario = :idDocente
        and c.periodo.idPeriodo = :idPeriodo
        and c.estado = com.uniautonoma.docentia.entity.EstadoCita.ACTIVA
      """)
  Optional<Cita> bloquearActiva(Long idDocente, Long idPeriodo);

  /** Cita activa del docente en el periodo, con la franja y el coordinador cargados. */
  @Query(
      """
      select c from Cita c join fetch c.franja f join fetch f.coordinador
      where c.docente.idUsuario = :idDocente
        and c.periodo.idPeriodo = :idPeriodo
        and c.estado = com.uniautonoma.docentia.entity.EstadoCita.ACTIVA
      """)
  Optional<Cita> buscarActiva(Long idDocente, Long idPeriodo);

  @Query(
      """
      select c from Cita c join fetch c.docente
      where c.franja.idFranja in :idsFranjas and c.estado = :estado
      """)
  List<Cita> buscarPorFranjas(Collection<Long> idsFranjas, EstadoCita estado);
}
