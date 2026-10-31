package com.uniautonoma.docentia.entity;

/**
 * Avisos del sistema. Los textos son fijos a propósito: así ninguna nota, categoría, comentario o
 * hallazgo puede terminar en una notificación o en un correo.
 */
public enum TipoNotificacion {
  HALLAZGOS_PUBLICADOS(
      "Resultados disponibles", "La coordinación publicó tus resultados. Ingresa para verlos."),
  CITA_AGENDADA("Cita agendada", "Tu cita de retroalimentación quedó agendada."),
  CITA_CAMBIADA("Cita cambiada", "Tu cita de retroalimentación cambió de horario."),
  RECORDATORIO_CITA("Recordatorio de cita", "Tienes una cita de retroalimentación en 24 horas."),
  ACTA_POR_FIRMAR(
      "Acta pendiente de firma", "Tienes un acta pendiente de firma. Ingresa para verla."),
  ACTA_FIRMADA("Acta firmada", "Tu acta quedó firmada por las dos partes."),
  ACTA_CORREGIDA("Acta corregida", "Hay una nueva versión de tu acta. Ingresa para verla."),
  COMPROMISO_POR_VENCER("Compromiso próximo a vencer", "Tienes un compromiso próximo a vencer."),
  COMPROMISO_VENCIDO("Compromiso vencido", "Tienes un compromiso vencido."),
  ANALISIS_TERMINADO("Análisis terminado", "El análisis de comentarios está listo para revisión."),
  ANALISIS_INSUFICIENTE(
      "Evidencia insuficiente", "El análisis de comentarios no tuvo evidencia suficiente."),
  ANALISIS_ERROR("Análisis con error", "El análisis de comentarios no se pudo completar."),
  CITA_RESERVADA("Cita reservada", "Un docente de tu facultad reservó una cita."),
  CITA_REEMPLAZADA("Cita liberada", "Un docente de tu facultad liberó su cita para tomar otra."),
  ACTA_FIRMADA_DOCENTE("Acta firmada por el docente", "Un docente de tu facultad firmó su acta."),
  COMPROMISO_VENCIDO_FACULTAD(
      "Compromiso vencido en tu facultad", "Un compromiso de tu facultad está vencido."),
  IMPORTACION_EXITOSA("Importación terminada", "Tu carga de CSV terminó correctamente."),
  IMPORTACION_CON_ERRORES("Importación con errores", "Tu carga de CSV fue rechazada por errores."),
  VENTANA_POR_CERRAR(
      "Etapa próxima a cerrar",
      "Una etapa del periodo cierra en 3 días. Revisa los pendientes de tu facultad."),
  ALERTA_INFORMATIVA(
      "Alerta informativa", "Hay una nueva alerta informativa en acciones sugeridas."),
  VENTANA_CERRADA("Etapa cerrada", "Una etapa del periodo se cerró. Revisa el panel por programa."),
  VENTANA_ABIERTA("Etapa abierta", "Se abrió una etapa del periodo en la que participas.");

  private final String titulo;
  private final String mensaje;

  TipoNotificacion(String titulo, String mensaje) {
    this.titulo = titulo;
    this.mensaje = mensaje;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getMensaje() {
    return mensaje;
  }
}
