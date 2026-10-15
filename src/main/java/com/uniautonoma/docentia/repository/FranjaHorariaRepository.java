package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.EstadoFranja;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Acceso a las franjas horarias de los coordinadores. */
public interface FranjaHorariaRepository extends JpaRepository<FranjaHoraria, Long> {

  Optional<FranjaHoraria> findByIdFranjaAndCoordinadorIdUsuario(Long idFranja, Long idCoordinador);

  /**
   * Lista las franjas de un coordinador en un periodo, entre dos fechas y con ciertos estados. Los
   * filtros son siempre valores concretos: con "parámetro is null" PostgreSQL no logra deducir el
   * tipo de una fecha nula y la consulta falla.
   *
   * @return ordenadas por fecha y hora
   */
  @Query(
      """
      select f from FranjaHoraria f
      where f.coordinador.idUsuario = :idCoordinador
        and f.periodo.idPeriodo = :idPeriodo
        and f.fecha between :desde and :hasta
        and f.estado in :estados
      order by f.fecha, f.horaInicio
      """)
  List<FranjaHoraria> buscarDelCoordinador(
      Long idCoordinador,
      Long idPeriodo,
      LocalDate desde,
      LocalDate hasta,
      Collection<EstadoFranja> estados);

  /**
   * Busca una franja abierta del coordinador que se cruce con el horario dado. Es la misma regla
   * que la restricción de exclusión de la base; aquí sirve para responder con un mensaje claro.
   */
  @Query(
      value =
          """
          select * from franjas_horarias
          where id_coordinador = :idCoordinador
            and estado = 'ABIERTA'
            and tsrange(fecha + hora_inicio, fecha + hora_fin)
                && tsrange(cast(:fecha as date) + cast(:inicio as time),
                           cast(:fecha as date) + cast(:fin as time))
          limit 1
          """,
      nativeQuery = true)
  Optional<FranjaHoraria> buscarSolapada(
      Long idCoordinador, LocalDate fecha, LocalTime inicio, LocalTime fin);
}
