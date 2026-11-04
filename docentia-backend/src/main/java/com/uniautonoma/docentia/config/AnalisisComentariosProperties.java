package com.uniautonoma.docentia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parámetros del análisis de comentarios. Los valores por defecto están explicados en
 * docs/ANALISIS_IA_CRITERIOS.md.
 *
 * @param lexico recurso del classpath con el léxico versionado del modelo local
 * @param minimoRecurrencia comentarios mínimos para que un tema sea recurrente
 * @param porcentajeRecurrencia fracción mínima de los comentarios útiles para que sea recurrente
 * @param maximoPorLista fortalezas u oportunidades que se muestran como máximo
 * @param umbralTendencia puntos porcentuales para hablar de aumento o disminución
 * @param maximoRepeticiones copias idénticas de un comentario que se conservan
 * @param maximoComentarios comentarios que se analizan como máximo en una corrida
 * @param minimoDocentesResumen docentes con análisis necesarios para mostrar el resumen agregado
 * @param consultasPorMinuto consultas por usuario y minuto a los endpoints de comentarios
 * @param tamanoMaximoPagina tamaño máximo de página del listado de comentarios
 */
@ConfigurationProperties(prefix = "docentia.ia.analisis")
public record AnalisisComentariosProperties(
    @DefaultValue("ia/lexico-v1.txt") String lexico,
    @DefaultValue("3") int minimoRecurrencia,
    @DefaultValue("0.2") double porcentajeRecurrencia,
    @DefaultValue("5") int maximoPorLista,
    @DefaultValue("10") double umbralTendencia,
    @DefaultValue("3") int maximoRepeticiones,
    @DefaultValue("2000") int maximoComentarios,
    @DefaultValue("3") int minimoDocentesResumen,
    @DefaultValue("60") int consultasPorMinuto,
    @DefaultValue("50") int tamanoMaximoPagina) {}
