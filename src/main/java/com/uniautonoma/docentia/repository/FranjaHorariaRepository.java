package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.EstadoFranja;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** Acceso a las franjas horarias de los coordinadores. */
public interface FranjaHorariaRepository extends JpaRepository<FranjaHoraria, Long> {

  Optional<FranjaHoraria> findByIdFranjaAndCoordinadorIdUsuario(Long idFranja, Long idCoordinador);

  /**
   * Lee la franja bloqueando su fila (SELECT … FOR UPDATE) hasta el final de la transacción: dos
   * docentes que reservan la misma franja a la vez quedan en fila y el segundo ve la reserva del
   * primero.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select f from FranjaHoraria f where f.idFranja = :idFranja")
  Optional<FranjaHoraria> bloquear(Long idFranja);

  /**
   * Franjas que un docente puede reservar: abiertas, de hoy en adelante, del periodo, de un
   * coordinador de su facultad y sin cita activa.
   */
  @Query(
      """
      select f from FranjaHoraria f join fetch f.coordinador c
      where f.periodo.idPeriodo = :idPeriodo
        and f.estado = com.uniautonoma.docentia.entity.EstadoFranja.ABIERTA
        and f.fecha >= :desde
        and c.facultad.idFacultad = :idFacultad
        and not exists (
            select 1 from Cita ci
            where ci.franja = f and ci.estado = com.uniautonoma.docentia.entity.EstadoCita.ACTIVA)
      order by f.fecha, f.horaInicio
      """)
  List<FranjaHoraria> buscarDisponibles(Long idPeriodo, Long idFacultad, LocalDate desde);

  /**
   * Lista las franjas de un coordinador en un periodo, opcionalmente de un día y un estado.
   *
   * @return ordenadas por fecha y hora
   */
  @Query(
      """
      select f from FranjaHoraria f
      where f.coordinador.idUsuario = :idCoordinador
        and f.periodo.idPeriodo = :idPeriodo
        and (:fecha is null or f.fecha = :fecha)
        and (:estado is null or f.estado = :estado)
      order by f.fecha, f.horaInicio
      """)
  List<FranjaHoraria> buscarDelCoordinador(
      Long idCoordinador, Long idPeriodo, LocalDate fecha, EstadoFranja estado);

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
