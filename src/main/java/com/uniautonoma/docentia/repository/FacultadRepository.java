package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Facultad;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de las facultades. */
public interface FacultadRepository extends JpaRepository<Facultad, Long> {}
