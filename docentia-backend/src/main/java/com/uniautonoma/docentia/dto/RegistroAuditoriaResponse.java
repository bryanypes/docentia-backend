package com.uniautonoma.docentia.dto;

import java.time.LocalDateTime;

/** Una fila de auditoría. No lleva datos de la evaluación. */
public record RegistroAuditoriaResponse(
    Long id,
    String correo,
    String rol,
    String accion,
    String entidad,
    Long entidadId,
    LocalDateTime fecha,
    String ip,
    String resultado) {}
