package com.uniautonoma.docentia.entity;

public enum ResultadoImportacion {
  /** Todas las filas pasaron la validación y se guardaron. */
  EXITOSA,
  /** Hubo al menos un error; no se guardó ninguna fila. */
  RECHAZADA
}
