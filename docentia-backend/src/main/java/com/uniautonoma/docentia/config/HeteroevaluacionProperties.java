package com.uniautonoma.docentia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Configuration;

/**
 * Heteroevaluación por formulario.
 *
 * @param minimoRespuestas por debajo de esto no se muestra ni se usa ningún agregado (k-anonimato)
 * @param lecturasPorMinuto tope de lecturas del formulario por estudiante
 * @param enviosPorMinuto tope de envíos por estudiante
 * @param tamanoMaximoMatriculaBytes tope del CSV de matrícula
 * @param filasMaximasMatricula tope de filas del CSV de matrícula
 * @param bloqueRespuestas con la recolección abierta, la nota de estudiantes se publica al llegar
 *     al mínimo y luego cada tantas respuestas más (5, 9, 13… con mínimo 5 y bloque 4): ningún
 *     cambio de nota corresponde a una sola respuesta. Al cerrar se consolida con todas
 */
@ConfigurationProperties(prefix = "docentia.heteroevaluacion")
public record HeteroevaluacionProperties(
    @DefaultValue("5") int minimoRespuestas,
    @DefaultValue("120") int lecturasPorMinuto,
    @DefaultValue("20") int enviosPorMinuto,
    @DefaultValue("5242880") long tamanoMaximoMatriculaBytes,
    @DefaultValue("20000") int filasMaximasMatricula,
    @DefaultValue("4") int bloqueRespuestas) {

  @Configuration
  @EnableConfigurationProperties(HeteroevaluacionProperties.class)
  static class Registro {}
}
