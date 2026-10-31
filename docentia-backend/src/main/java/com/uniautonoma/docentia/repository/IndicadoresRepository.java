package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.PromedioDocente;
import java.math.BigDecimal;
import java.util.List;
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

  /**
   * Resume cada programa: docentes activos, evaluados con promedio completo y su distribución. Solo
   * devuelve agregados, nunca filas de docentes.
   *
   * @param idFacultad facultad del programa, o nulo para toda la institución
   */
  @Query(
      value =
          """
          select pr.id_programa as idPrograma,
                 pr.nombre      as programa,
                 f.nombre       as facultad,
                 count(distinct u.id_usuario)                                  as docentes,
                 count(p.id_promedio) filter (where p.estado = 'COMPLETO')     as evaluados,
                 sum(p.valor) filter (where p.estado = 'COMPLETO')             as sumaNotas,
                 count(*) filter (where p.estado = 'COMPLETO'
                                    and p.categoria = 'MUY_ALTO')              as muyAlto,
                 count(*) filter (where p.estado = 'COMPLETO'
                                    and p.categoria = 'ALTO')                  as alto,
                 count(*) filter (where p.estado = 'COMPLETO'
                                    and p.categoria = 'ACEPTABLE')             as aceptable,
                 count(*) filter (where p.estado = 'COMPLETO'
                                    and p.categoria = 'BAJO')                  as bajo,
                 count(*) filter (where p.estado = 'COMPLETO'
                                    and p.categoria = 'MUY_BAJO')              as muyBajo
          from programas pr
          join facultades f on f.id_facultad = pr.id_facultad
          join usuarios u on u.id_programa = pr.id_programa
                         and u.rol = 'DOCENTE' and u.es_activo
          left join promedios_docente p on p.id_docente = u.id_usuario
                                       and p.id_periodo = :idPeriodo
          where cast(:idFacultad as bigint) is null or pr.id_facultad = :idFacultad
          group by pr.id_programa, pr.nombre, f.nombre
          order by f.nombre, pr.nombre
          """,
      nativeQuery = true)
  List<ResumenPrograma> resumirPorPrograma(Long idPeriodo, Long idFacultad);

  /** Fila de {@link #resumirPorPrograma}. */
  interface ResumenPrograma {

    Long getIdPrograma();

    String getPrograma();

    String getFacultad();

    Long getDocentes();

    Long getEvaluados();

    BigDecimal getSumaNotas();

    Long getMuyAlto();

    Long getAlto();

    Long getAceptable();

    Long getBajo();

    Long getMuyBajo();
  }

  /** Resultado de {@link #resumirPromedios}; el promedio es nulo si no hay promedios completos. */
  interface ResumenPromedios {

    Long getDocentesEvaluados();

    BigDecimal getPromedioGeneral();

    Long getDocentesDestacados();
  }
}
