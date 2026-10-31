package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.SeguimientoCompromiso;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso al historial de cambios de los compromisos. */
public interface SeguimientoCompromisoRepository
    extends JpaRepository<SeguimientoCompromiso, Long> {

  @EntityGraph(attributePaths = "usuario")
  List<SeguimientoCompromiso> findByCompromisoIdCompromisoOrderByFechaDesc(Long idCompromiso);
}
