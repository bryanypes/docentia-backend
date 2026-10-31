package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.TipoEtapa;
import java.time.LocalDate;

/**
 * Estado de una etapa del periodo activo hoy. Las fechas son nulas si la etapa no está programada,
 * y en ese caso la etapa cuenta como cerrada.
 */
public record VentanaEtapaResponse(
    TipoEtapa etapa,
    String nombreEtapa,
    LocalDate fechaApertura,
    LocalDate fechaCierre,
    boolean abierta) {}
