package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.PlantillaMapeo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a las plantillas de mapeo de columnas de cada usuario. */
public interface PlantillaMapeoRepository extends JpaRepository<PlantillaMapeo, Long> {

  List<PlantillaMapeo> findByUsuarioIdUsuarioOrderByFechaActualizacionDesc(Long idUsuario);

  Optional<PlantillaMapeo> findByUsuarioIdUsuarioAndNombreIgnoreCase(Long idUsuario, String nombre);

  Optional<PlantillaMapeo> findByIdPlantillaAndUsuarioIdUsuario(Long idPlantilla, Long idUsuario);
}
