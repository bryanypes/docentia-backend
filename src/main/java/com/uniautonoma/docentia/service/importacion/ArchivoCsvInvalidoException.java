package com.uniautonoma.docentia.service.importacion;

/** El archivo no se puede leer como CSV; el mensaje se muestra tal cual en el reporte. */
public class ArchivoCsvInvalidoException extends RuntimeException {

  public ArchivoCsvInvalidoException(String mensaje) {
    super(mensaje);
  }
}
