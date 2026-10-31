package com.uniautonoma.docentia.service.analisis;

import java.util.List;

/**
 * De dónde salen los comentarios de los estudiantes. Hoy son los cargados por CSV (C4); cuando
 * llegue el token de la API de Campus se agrega otra implementación sin tocar el análisis.
 */
public interface ProveedorComentarios {

  /**
   * Cuenta las encuestas cerradas del docente en el periodo, que deciden si hay evidencia
   * suficiente para analizar.
   */
  int contarEncuestasCerradas(Long idDocente, Long idPeriodo);

  /** Comentarios del docente en el periodo, en el orden en que se cargaron. */
  List<ComentarioFuente> comentarios(Long idDocente, Long idPeriodo);
}
