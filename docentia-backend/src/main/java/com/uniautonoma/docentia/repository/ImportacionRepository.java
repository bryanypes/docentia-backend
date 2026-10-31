package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Importacion;
import com.uniautonoma.docentia.entity.ResultadoImportacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Acceso al registro de cargas de CSV. */
public interface ImportacionRepository extends JpaRepository<Importacion, Long> {

  /**
   * Lista las cargas, opcionalmente de un usuario y de un periodo, con periodo y usuario cargados.
   *
   * @param idUsuario usuario que cargó, o nulo para todos
   * @param idPeriodo periodo, o nulo para todos
   * @return cargas de la más reciente a la más antigua
   */
  @Query(
      """
      select i from Importacion i
      join fetch i.periodo
      join fetch i.usuario
      where (:idUsuario is null or i.usuario.idUsuario = :idUsuario)
        and (:idPeriodo is null or i.periodo.idPeriodo = :idPeriodo)
      order by i.fechaImportacion desc, i.idImportacion desc
      """)
  List<Importacion> buscar(Long idUsuario, Long idPeriodo);

  /**
   * Busca la última carga exitosa del mismo archivo en el periodo, para avisar que se repite.
   *
   * @return la carga anterior más reciente, si existe
   */
  Optional<Importacion>
      findFirstByHashSha256AndPeriodoIdPeriodoAndResultadoOrderByIdImportacionDesc(
          String hashSha256, Long idPeriodo, ResultadoImportacion resultado);
}
