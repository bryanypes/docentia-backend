package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.PromedioDocente;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Consultas agregadas para los indicadores generales. Cada una resuelve un periodo y un alcance en
 * una sola sentencia, sin recorrer docentes uno por uno.
 */
public interface IndicadoresRepository extends Repository<PromedioDocente, Long> {

  /**
   * Resume los promedios completos de un periodo.
   *
   * @param idFacultad facultad del programa del docente, o nulo para toda la institución
   * @param umbral promedio mínimo, inclusive, de un docente destacado
   */
  @Query(
      value =
          """
          select count(*)                                          as docentesEvaluados,
                 round(avg(p.valor), 2)                            as promedioGeneral,
                 count(*) filter (where p.valor >= :umbral)        as docentesDestacados
          from promedios_docente p
          join usuarios u on u.id_usuario = p.id_docente
          join programas pr on pr.id_programa = u.id_programa
          where p.id_periodo = :idPeriodo
            and p.estado = 'COMPLETO'
            and (cast(:idFacultad as bigint) is null or pr.id_facultad = :idFacultad)
          """,
      nativeQuery = true)
  ResumenPromedios resumirPromedios(Long idPeriodo, Long idFacultad, BigDecimal umbral);

  /**
   * Cuenta las calificaciones registradas en un periodo, de cualquier fuente.
   *
   * @param idFacultad facultad del programa del docente, o nulo para toda la institución
   */
  @Query(
      value =
          """
          select count(*)
          from calificaciones c
          join usuarios u on u.id_usuario = c.id_docente
          join programas pr on pr.id_programa = u.id_programa
          where c.id_periodo = :idPeriodo
            and (cast(:idFacultad as bigint) is null or pr.id_facultad = :idFacultad)
          """,
      nativeQuery = true)
  long contarCalificaciones(Long idPeriodo, Long idFacultad);

  /** Resultado de {@link #resumirPromedios}; el promedio es nulo si no hay promedios completos. */
  interface ResumenPromedios {

    Long getDocentesEvaluados();

    BigDecimal getPromedioGeneral();

    Long getDocentesDestacados();
  }
}
