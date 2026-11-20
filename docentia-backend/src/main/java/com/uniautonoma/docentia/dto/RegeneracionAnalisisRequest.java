package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.FuenteComentarios;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Pedido explícito de volver a generar un análisis.
 *
 * @param motivo opcional: por qué se vuelve a analizar; queda guardado en la solicitud
 * @param modelo LOCAL o LLM; nulo usa el configurado por defecto
 */
public record RegeneracionAnalisisRequest(
    @NotNull(message = "El periodo es obligatorio") Long periodoId,
    @Size(max = 300, message = "El motivo admite hasta 300 caracteres") String motivo,
    FuenteComentarios fuente,
    @Pattern(regexp = "LOCAL|LLM", message = "El modelo debe ser LOCAL o LLM") String modelo) {}
