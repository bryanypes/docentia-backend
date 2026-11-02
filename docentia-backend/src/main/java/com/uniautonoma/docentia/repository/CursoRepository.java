package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Curso;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a asignaturas identificadas por código Campus. */
public interface CursoRepository extends JpaRepository<Curso, Long> {

  Optional<Curso> findByCodigoIgnoreCase(String codigo);
}
