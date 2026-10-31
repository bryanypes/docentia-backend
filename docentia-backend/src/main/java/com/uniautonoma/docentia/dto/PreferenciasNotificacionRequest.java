package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.NotNull;

/** Interruptor global de avisos por correo. */
public record PreferenciasNotificacionRequest(
    @NotNull(message = "Indica si quieres recibir correos") Boolean correoHabilitado) {}
