package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Acta;
import com.uniautonoma.docentia.entity.AsistenciaCita;
import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.EstadoActa;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Lecturas para el estado del proceso de varios docentes a la vez: una consulta por tipo de dato,
 * sin recorrer docentes uno por uno.
 */
public interface ProcesoEvaluacionRepository extends Repository<Cita, Long> {

  boolean existsByDocenteIdUsuarioAndPeriodoIdPeriodoAndAsistencia(
      Long idDocente, Long idPeriodo, AsistenciaCita asistencia);

  @Query(
      """
      select count(a) > 0 from Acta a
      where a.docente.idUsuario = :idDocente
        and a.periodo.idPeriodo = :idPeriodo
        and a.estado = :estado
      """)
  boolean existeActaEnEstado(Long idDocente, Long idPeriodo, EstadoActa estado);

  /** Todas las citas de los docentes en el periodo, activas o liberadas, de la más antigua. */
  @Query(
      """
      select c from Cita c
      join fetch c.franja f
      join fetch f.coordinador
      where c.periodo.idPeriodo = :idPeriodo and c.docente.idUsuario in :idsDocentes
      order by c.fechaReserva asc, c.idCita asc
      """)
  List<Cita> buscarCitas(Long idPeriodo, Collection<Long> idsDocentes);

  /** Actas de los docentes en el periodo con sus firmas, de la versión más alta a la más baja. */
  @Query(
      """
      select distinct a from Acta a
      left join fetch a.firmas
      where a.periodo.idPeriodo = :idPeriodo and a.docente.idUsuario in :idsDocentes
      order by a.version desc
      """)
  List<Acta> buscarActas(Long idPeriodo, Collection<Long> idsDocentes);

  /**
   * Cobertura de la heteroevaluación con el mismo criterio de {@code CoberturaService}: matrículas
   * activas en las asignaciones del docente frente a participaciones. Solo cuenta; la participación
   * no se une con la respuesta, así que nunca dice quién puntuó qué.
   */
  @Query(
      value =
          """
          select u.id_usuario as idDocente,
                 (select count(*)
                    from matriculas m
                    join clases c on c.id = m.clase_id
                    join clases_docente cd on cd.clase_id = c.id
                   where cd.docente_id = u.id_usuario
                     and c.id_periodo = :idPeriodo
                     and m.es_activa) as esperadas,
                 (select count(*)
                    from participaciones_heteroevaluacion pa
                    join clases_docente cd on cd.id = pa.asignacion_id
                    join clases c on c.id = cd.clase_id
                    join matriculas m
                      on m.clase_id = c.id and m.estudiante_id = pa.estudiante_id and m.es_activa
                   where cd.docente_id = u.id_usuario
                     and c.id_periodo = :idPeriodo) as respondidas
          from usuarios u
          where u.id_usuario in (:idsDocentes)
          """,
      nativeQuery = true)
  List<CoberturaEstudiantes> contarCobertura(Long idPeriodo, Collection<Long> idsDocentes);

  /** Origen de la nota de estudiantes de cada docente en el periodo (MANUAL, CSV o FORMULARIO). */
  @Query(
      value =
          """
          select c.id_docente as idDocente, c.origen as origen
          from calificaciones c
          where c.id_periodo = :idPeriodo
            and c.fuente = 'ESTUDIANTES'
            and c.id_docente in (:idsDocentes)
          """,
      nativeQuery = true)
  List<OrigenNota> buscarOrigenEstudiantes(Long idPeriodo, Collection<Long> idsDocentes);

  interface OrigenNota {
    Long getIdDocente();

    String getOrigen();
  }

  interface CoberturaEstudiantes {
    Long getIdDocente();

    long getEsperadas();

    long getRespondidas();
  }
}
