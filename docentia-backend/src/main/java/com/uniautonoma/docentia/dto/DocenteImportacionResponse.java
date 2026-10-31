package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.NotasAspectos;
import com.uniautonoma.docentia.service.importacion.ValidadorEvaluacionEstudiantes.DatosDocente;
import java.math.BigDecimal;

/**
 * Vista previa de lo que la carga guardará para un docente. Las notas son nulas si el archivo solo
 * trae comentarios suyos.
 */
public record DocenteImportacionResponse(
    Long idDocente,
    String nombreCompleto,
    String correo,
    int filas,
    int filasConNotas,
    int comentarios,
    BigDecimal dominioDisciplinar,
    BigDecimal planeacionAcademica,
    BigDecimal metodologiaEnsenanza,
    BigDecimal evaluacionAprendizaje,
    BigDecimal compromisoInstitucional,
    BigDecimal notaFuente) {

  public static DocenteImportacionResponse desde(DatosDocente datos) {
    NotasAspectos notas = datos.notas();
    return new DocenteImportacionResponse(
        datos.docente().getIdUsuario(),
        datos.docente().getNombreCompleto(),
        datos.docente().getCorreo(),
        datos.filas(),
        datos.filasConNotas(),
        datos.comentarios().size(),
        notas == null ? null : notas.dominioDisciplinar(),
        notas == null ? null : notas.planeacionAcademica(),
        notas == null ? null : notas.metodologiaEnsenanza(),
        notas == null ? null : notas.evaluacionAprendizaje(),
        notas == null ? null : notas.compromisoInstitucional(),
        notas == null ? null : notas.promedio());
  }
}
