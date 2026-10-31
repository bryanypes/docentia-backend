package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.PeriodoAcademico;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Acceso a datos de los periodos académicos. */
public interface PeriodoAcademicoRepository extends JpaRepository<PeriodoAcademico, Long> {

  /**
   * Lista todos los periodos, del más reciente al más antiguo.
   *
   * @return periodos ordenados por fecha de inicio descendente
   */
  List<PeriodoAcademico> findAllByOrderByFechaInicioDesc();

  /**
   * Busca el periodo que empezó justo antes de una fecha.
   *
   * @param fechaInicio fecha de inicio del periodo de referencia
   * @return el periodo anterior más cercano, si existe
   */
  Optional<PeriodoAcademico> findFirstByFechaInicioLessThanOrderByFechaInicioDesc(
      LocalDate fechaInicio);

  /**
   * Busca el periodo que está activo.
   *
   * @return el periodo activo, o vacío si no hay ninguno
   */
  Optional<PeriodoAcademico> findByEsActivoTrue();

  /**
   * Busca un periodo por su nombre exacto.
   *
   * @param nombre nombre del periodo, por ejemplo 2026-2
   * @return el periodo con ese nombre, si existe
   */
  Optional<PeriodoAcademico> findByNombre(String nombre);

  /**
   * Desactiva el periodo que esté activo. Se ejecuta de inmediato para que la activación siguiente
   * no choque con el índice de periodo único activo.
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("update PeriodoAcademico p set p.esActivo = false where p.esActivo = true")
  void desactivarPeriodoActivo();
}
