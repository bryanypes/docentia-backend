package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.dto.PromedioPonderadoResponse;
import com.uniautonoma.docentia.entity.PromedioDocente;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Acceso a datos de los promedios ponderados guardados. */
public interface PromedioDocenteRepository extends JpaRepository<PromedioDocente, Long> {

  /**
   * Busca la entidad del promedio guardado de un docente en un periodo.
   *
   * @param idDocente identificador del docente
   * @param idPeriodo identificador del periodo
   * @return el promedio, o vacío si el docente no tiene calificaciones en ese periodo
   */
  Optional<PromedioDocente> findByDocenteIdUsuarioAndPeriodoIdPeriodo(
      Long idDocente, Long idPeriodo);

  /**
   * Lee el promedio guardado sin cargar la entidad, así nunca se responde con una copia vieja que
   * haya quedado en memoria después de {@link #guardar}.
   *
   * @param idDocente identificador del docente
   * @param idPeriodo identificador del periodo
   * @return el promedio, o vacío si el docente no tiene calificaciones en ese periodo
   */
  @Query(
      """
      select new com.uniautonoma.docentia.dto.PromedioPonderadoResponse(
          p.estado, p.valor, p.fuentesPendientes)
      from PromedioDocente p
      where p.docente.idUsuario = :idDocente and p.periodo.idPeriodo = :idPeriodo
      """)
  Optional<PromedioPonderadoResponse> leerPromedio(Long idDocente, Long idPeriodo);

  /**
   * Inserta o reemplaza el promedio de un docente en un periodo en una sola sentencia. Con la
   * restricción única por docente y periodo, dos recálculos simultáneos no pueden crear dos filas
   * ni fallar por duplicado: el segundo espera al primero y lo sobrescribe.
   *
   * @param fuentesPendientes fuentes separadas por coma, o nulo si el promedio está completo
   */
  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
          insert into promedios_docente
              (id_docente, id_periodo, valor, estado, fuentes_pendientes, fecha_calculo)
          values (:idDocente, :idPeriodo, cast(:valor as numeric), :estado,
                  cast(:fuentesPendientes as varchar), clock_timestamp())
          on conflict (id_docente, id_periodo) do update set
              valor = excluded.valor,
              estado = excluded.estado,
              fuentes_pendientes = excluded.fuentes_pendientes,
              fecha_calculo = excluded.fecha_calculo
          """,
      nativeQuery = true)
  void guardar(
      Long idDocente, Long idPeriodo, BigDecimal valor, String estado, String fuentesPendientes);
}
