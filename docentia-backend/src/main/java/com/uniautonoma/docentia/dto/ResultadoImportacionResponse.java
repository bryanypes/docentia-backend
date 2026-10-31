package com.uniautonoma.docentia.dto;

/**
 * Respuesta de importar: el registro de la carga y el reporte de validación. Si la carga fue
 * rechazada, el registro existe igual (resultado RECHAZADA) y no se guardó ninguna nota.
 */
public record ResultadoImportacionResponse(
    ImportacionResponse importacion, ReporteImportacionResponse reporte) {

  public boolean fueExitosa() {
    return reporte.valido();
  }
}
