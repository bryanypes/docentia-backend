package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Programa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de los programas académicos. */
public interface ProgramaRepository extends JpaRepository<Programa, Long> {

  /**
   * Lista los programas vigentes de todas las facultades.
   *
   * @return programas activos ordenados por nombre, con su facultad cargada
   */
  @EntityGraph(attributePaths = "facultad")
  List<Programa> findByEsActivoTrueOrderByNombreAsc();

  /**
   * Lista los programas vigentes de una facultad.
   *
   * @param idFacultad identificador de la facultad
   * @return programas activos de la facultad ordenados por nombre
   */
  @EntityGraph(attributePaths = "facultad")
  List<Programa> findByFacultadIdFacultadAndEsActivoTrueOrderByNombreAsc(Long idFacultad);

  /**
   * Busca un programa vigente por su identificador.
   *
   * @param idPrograma identificador del programa
   * @return el programa con su facultad, o vacío si no existe o fue dado de baja
   */
  @EntityGraph(attributePaths = "facultad")
  Optional<Programa> findByIdProgramaAndEsActivoTrue(Long idPrograma);

  /**
   * Indica si una facultad todavía tiene programas vigentes.
   *
   * @param idFacultad identificador de la facultad
   * @return verdadero si existe al menos un programa activo
   */
  boolean existsByFacultadIdFacultadAndEsActivoTrue(Long idFacultad);

  /**
   * Busca dentro de una facultad un programa vigente por nombre, sin distinguir mayúsculas.
   *
   * @param idFacultad identificador de la facultad
   * @param nombre nombre a buscar
   * @return el programa que ya usa ese nombre en la facultad, si lo hay
   */
  Optional<Programa> findByFacultadIdFacultadAndNombreIgnoreCaseAndEsActivoTrue(
      Long idFacultad, String nombre);
}
