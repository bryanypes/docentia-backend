package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.CorreccionCalificacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Acceso al historial de correcciones de calificaciones. */
public interface CorreccionCalificacionRepository
    extends JpaRepository<CorreccionCalificacion, Long> {

  /**
   * Lista las correcciones de un docente, con el periodo y quien corrigió ya cargados.
   *
   * @param idDocente identificador del docente
   * @param idPeriodo periodo por el que se filtra, o nulo para todos
   * @return correcciones de la más reciente a la más antigua
   */
  @Query(
      """
      select c from CorreccionCalificacion c
      join fetch c.periodo
      left join fetch c.usuario
      where c.docente.idUsuario = :idDocente
        and (:idPeriodo is null or c.periodo.idPeriodo = :idPeriodo)
      order by c.fechaCorreccion desc, c.idCorreccion desc
      """)
  List<CorreccionCalificacion> buscarPorDocente(Long idDocente, Long idPeriodo);
}
