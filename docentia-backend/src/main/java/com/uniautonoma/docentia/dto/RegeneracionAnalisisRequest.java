package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.FuenteComentarios;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Pedido explícito de volver a generar un análisis.
 *
 * @param motivo por qué se regenera; queda guardado en la solicitud
 * @param modelo LOCAL para usar el modelo por reglas; nulo usa el proveedor configurado
 */
public record RegeneracionAnalisisRequest(
    @NotNull(message = "El periodo es obligatorio") Long periodoId,
    @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 10, max = 300, message = "El motivo debe tener entre 10 y 300 caracteres")
        String motivo,
    FuenteComentarios fuente,
    @Pattern(regexp = "LOCAL", message = "El modelo solo puede ser LOCAL") String modelo) {}
