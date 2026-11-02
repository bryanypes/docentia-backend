package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.AsistenciaCita;
import com.uniautonoma.docentia.entity.ConfirmacionCita;
import com.uniautonoma.docentia.entity.EstadoActa;
import com.uniautonoma.docentia.entity.EstadoProcesoEvaluacion;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.TipoActa;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Estado general del proceso de un docente en el periodo activo, con su línea de tiempo. No lleva
 * notas: sirve igual a coordinación, vicerrectoría y al propio docente.
 *
 * @param fuentesPendientes fuentes que todavía no califican
 * @param coberturaEstudiantes formularios de estudiantes enviados frente a matrículas, o nula para
 *     el docente
 * @param cita cita vigente, o la última que tuvo si ya no tiene ninguna activa
 * @param acta versión vigente del acta, o nula si no se ha generado
 * @param resultadoHabilitado el docente ya puede ver su nota, compromisos y acta
 * @param hitos sucesos con fecha, del más antiguo al más reciente
 */
public record ProcesoEvaluacionResponse(
    DocenteResponse docente,
    EstadoProcesoEvaluacion estado,
    String nombreEstado,
    int paso,
    List<FuenteEvaluacion> fuentesPendientes,
    Cobertura coberturaEstudiantes,
    CitaProceso cita,
    ActaProceso acta,
    boolean resultadoHabilitado,
    List<Hito> hitos) {

  public record Cobertura(long respondidas, long esperadas) {}

  public record CitaProceso(
      Long idCita,
      boolean activa,
      LocalDate fecha,
      LocalTime horaInicio,
      LocalTime horaFin,
      String nombreCoordinador,
      ConfirmacionCita confirmacion,
      LocalDateTime fechaConfirmacion,
      AsistenciaCita asistencia,
      LocalDateTime fechaAsistencia) {}

  public record ActaProceso(
      Long idActa,
      TipoActa tipo,
      EstadoActa estado,
      boolean firmadaPorDocente,
      boolean firmadaPorCoordinacion,
      LocalDateTime fechaFirma) {}

  /**
   * Un suceso del proceso.
   *
   * @param codigo RESERVA, CAMBIO, DECLINACION, CONFIRMACION, ASISTENCIA, INASISTENCIA, ACTA, FIRMA
   *     o CIERRE
   */
  public record Hito(String codigo, String titulo, String detalle, LocalDateTime fecha) {}
}
