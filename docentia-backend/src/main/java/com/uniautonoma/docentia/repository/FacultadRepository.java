package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Facultad;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de las facultades. */
public interface FacultadRepository extends JpaRepository<Facultad, Long> {

  /**
   * Lista las facultades que no han sido dadas de baja.
   *
   * @return facultades activas ordenadas por nombre
   */
  List<Facultad> findByEsActivaTrueOrderByNombreAsc();

  /**
   * Busca una facultad vigente por su identificador.
   *
   * @param idFacultad identificador de la facultad
   * @return la facultad, o vacío si no existe o fue dada de baja
   */
  Optional<Facultad> findByIdFacultadAndEsActivaTrue(Long idFacultad);

  /**
   * Busca una facultad vigente por nombre, sin distinguir mayúsculas.
   *
   * @param nombre nombre a buscar
   * @return la facultad que ya usa ese nombre, si la hay
   */
  Optional<Facultad> findByNombreIgnoreCaseAndEsActivaTrue(String nombre);
}
