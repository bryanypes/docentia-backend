package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Programa;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de los programas académicos. */
public interface ProgramaRepository extends JpaRepository<Programa, Long> {}
