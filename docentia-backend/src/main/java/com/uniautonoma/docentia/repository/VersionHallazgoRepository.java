package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.VersionHallazgo;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a las versiones de los hallazgos. */
public interface VersionHallazgoRepository extends JpaRepository<VersionHallazgo, Long> {

  @EntityGraph(attributePaths = "usuario")
  List<VersionHallazgo> findByHallazgoIdHallazgoOrderByVersionAsc(Long idHallazgo);

  int countByHallazgoIdHallazgo(Long idHallazgo);
}
