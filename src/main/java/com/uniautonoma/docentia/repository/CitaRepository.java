package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.EstadoCita;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Acceso a las citas reservadas en las franjas. */
public interface CitaRepository extends JpaRepository<Cita, Long> {

  boolean existsByFranjaIdFranjaAndEstado(Long idFranja, EstadoCita estado);

  boolean existsByFranjaIdFranja(Long idFranja);

  @Query(
      """
      select c from Cita c join fetch c.docente
      where c.franja.idFranja in :idsFranjas and c.estado = :estado
      """)
  List<Cita> buscarPorFranjas(Collection<Long> idsFranjas, EstadoCita estado);
}
