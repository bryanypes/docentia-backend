package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Hallazgo;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a los hallazgos de los borradores de análisis. */
public interface HallazgoRepository extends JpaRepository<Hallazgo, Long> {}
