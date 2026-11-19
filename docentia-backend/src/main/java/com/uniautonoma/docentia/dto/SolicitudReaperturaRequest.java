package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.TipoFormulario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Pedido para corregir un formulario ya enviado del periodo activo.
 *
 * @param tipo AUTO (la propia autoevaluación) o COEVAL (una coevaluación que hizo quien pide)
 * @param idDocente docente coevaluado; se ignora en la autoevaluación
 * @param motivo qué hay que corregir y por qué
 */
public record SolicitudReaperturaRequest(
    @NotNull(message = "Indica qué formulario quieres corregir") TipoFormulario tipo,
    Long idDocente,
    @NotBlank(message = "Explica qué quieres corregir")
        @Size(min = 15, max = 500, message = "El motivo debe tener entre 15 y 500 caracteres")
        String motivo) {}
