package com.uniautonoma.docentia.repository;

/** Un docente en un periodo, para recorrer promedios sin cargar las entidades completas. */
public record DocentePeriodo(Long idDocente, Long idPeriodo) {}
