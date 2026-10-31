package com.uniautonoma.docentia.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Calendario del periodo activo. Las semanas son una etiqueta informativa de la Resolución 074; lo
 * que abre o cierra una etapa son sus fechas.
 *
 * @param etapaVigente etapa abierta más avanzada, o nula si hoy no hay ninguna abierta
 * @param seguimiento cuándo se revisa el plan de mejoramiento
 */
public record CalendarioResponse(
    PeriodoResumenResponse periodo,
    LocalDate hoy,
    VentanaEtapaResponse etapaVigente,
    List<VentanaEtapaResponse> etapas,
    String seguimiento) {}
