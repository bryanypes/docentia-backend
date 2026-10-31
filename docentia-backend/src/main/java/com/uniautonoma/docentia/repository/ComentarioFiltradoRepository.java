package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.ComentarioFiltrado;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Registro de los comentarios excluidos por el filtro en cada solicitud de análisis. */
public interface ComentarioFiltradoRepository extends JpaRepository<ComentarioFiltrado, Long> {

  @Query(
      """
      select f from ComentarioFiltrado f join fetch f.comentario
      where f.solicitud.idSolicitud = :idSolicitud
      order by f.comentario.idComentario
      """)
  List<ComentarioFiltrado> buscarPorSolicitud(Long idSolicitud);

  @Query(
      "select f.comentario.idComentario from ComentarioFiltrado f"
          + " where f.solicitud.idSolicitud = :idSolicitud")
  List<Long> idsFiltrados(Long idSolicitud);
}
