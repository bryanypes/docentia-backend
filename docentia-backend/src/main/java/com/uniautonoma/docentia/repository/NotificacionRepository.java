package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Notificacion;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/** Bandeja de avisos de cada usuario. */
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

  Page<Notificacion> findByIdUsuarioOrderByCreadaEnDescIdDesc(Long idUsuario, Pageable pagina);

  Page<Notificacion> findByIdUsuarioAndLeidaOrderByCreadaEnDescIdDesc(
      Long idUsuario, boolean leida, Pageable pagina);

  long countByIdUsuarioAndLeidaFalse(Long idUsuario);

  Optional<Notificacion> findByIdAndIdUsuario(Long id, Long idUsuario);

  @Modifying
  @Query(
      "update Notificacion n set n.leida = true, n.leidaEn = :ahora"
          + " where n.idUsuario = :idUsuario and n.leida = false")
  int marcarTodasLeidas(Long idUsuario, LocalDateTime ahora);

  @Transactional
  @Modifying
  @Query("delete from Notificacion n where n.leida = true and n.leidaEn < :limite")
  int borrarLeidasAntesDe(LocalDateTime limite);
}
