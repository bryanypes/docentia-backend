package com.uniautonoma.docentia.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Lo que el acta imprime y no debe cambiar después de firmada: identificación del docente, periodo,
 * reunión, pesos, número de respuestas y aspectos conversados. Se copia al crear el acta y se
 * actualiza hasta la primera firma.
 *
 * @param documentoRotulo cómo se llama el documento en el acta (configurable)
 * @param docenteDocumento código del docente en {@code usuarios.codigo}; nulo si no tiene o si la
 *     institución no lo imprime
 * @param programaNombre nulo para un coordinador evaluado, que no tiene programa
 * @param reunionAsistencia ASISTIO o NO_ASISTIO; nulo si aún no se registra
 * @param respuestasEstudiantes respuestas del formulario de estudiantes; nunca va al docente
 * @param formatoCodigo código del formato institucional; nulo mientras no exista
 */
public record DatosCongeladosActa(
    String docenteNombre,
    String docenteCorreo,
    String documentoRotulo,
    String docenteDocumento,
    String docenteCargo,
    String programaNombre,
    String escuelaNombre,
    String periodoNombre,
    LocalDate periodoInicio,
    LocalDate periodoFin,
    String evaluadorCargo,
    LocalDate reunionFecha,
    LocalTime reunionHoraInicio,
    LocalTime reunionHoraFin,
    String reunionAsistencia,
    BigDecimal pesoEstudiantes,
    BigDecimal pesoCoordinacion,
    BigDecimal pesoAutoevaluacion,
    Integer respuestasEstudiantes,
    List<String> fortalezas,
    List<String> aspectosPorMejorar,
    String formatoCodigo,
    String formatoVersion) {

  public DatosCongeladosActa {
    fortalezas = fortalezas == null ? List.of() : List.copyOf(fortalezas);
    aspectosPorMejorar = aspectosPorMejorar == null ? List.of() : List.copyOf(aspectosPorMejorar);
  }
}
