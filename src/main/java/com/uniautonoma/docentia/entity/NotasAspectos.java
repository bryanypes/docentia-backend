package com.uniautonoma.docentia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** Los cinco aspectos que se califican de 1.0 a 5.0 en cada fuente de evaluación. */
@Embeddable
public record NotasAspectos(
    @Column(nullable = false, precision = 3, scale = 2) BigDecimal dominioDisciplinar,
    @Column(nullable = false, precision = 3, scale = 2) BigDecimal planeacionAcademica,
    @Column(nullable = false, precision = 3, scale = 2) BigDecimal metodologiaEnsenanza,
    @Column(nullable = false, precision = 3, scale = 2) BigDecimal evaluacionAprendizaje,
    @Column(nullable = false, precision = 3, scale = 2) BigDecimal compromisoInstitucional) {}
