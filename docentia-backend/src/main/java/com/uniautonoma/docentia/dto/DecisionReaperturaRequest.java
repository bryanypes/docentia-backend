package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Decisión sobre una solicitud de corrección: la del superior que la verifica o la de la
 * administración que la atiende.
 *
 * @param aprobada verdadero para aprobar (o reabrir el formulario); falso para rechazarla
 * @param respuesta explicación para el solicitante; obligatoria al rechazar
 */
public record DecisionReaperturaRequest(
    @NotNull(message = "Indica si apruebas o rechazas la solicitud") Boolean aprobada,
    @Size(max = 500, message = "La respuesta admite hasta 500 caracteres") String respuesta) {}
