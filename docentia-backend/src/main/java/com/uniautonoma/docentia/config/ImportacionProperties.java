package com.uniautonoma.docentia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Límites de la carga de CSV.
 *
 * @param tamanoMaximoBytes tamaño máximo del archivo; un export de Power BI de una facultad pesa
 *     unos pocos cientos de KB
 * @param filasMaximas filas de datos máximas por archivo
 */
@ConfigurationProperties(prefix = "docentia.importacion")
public record ImportacionProperties(
    @DefaultValue("2097152") long tamanoMaximoBytes, @DefaultValue("5000") int filasMaximas) {}
