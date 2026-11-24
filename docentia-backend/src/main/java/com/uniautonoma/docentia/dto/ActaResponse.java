package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Acta;
import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.DatosCongeladosActa;
import com.uniautonoma.docentia.entity.EstadoActa;
import com.uniautonoma.docentia.entity.FirmaActa;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.TipoActa;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Acta sin notas por fuente: la consulta también el docente (A4). */
public record ActaResponse(
    Long idActa,
    Long idDocente,
    String nombreDocente,
    PeriodoResumenResponse periodo,
    int version,
    EstadoActa estado,
    TipoActa tipo,
    BigDecimal promedio,
    EstadoPromedio estadoPromedio,
    CategoriaResponse categoria,
    BigDecimal umbralCompromisos,
    boolean requiereCompromisos,
    List<CompromisoActa> compromisos,
    List<Firma> firmas,
    String codigoVerificacion,
    String hashContenido,
    String hashPdf,
    LocalDateTime fechaFirma,
    Long idActaAnterior,
    String observaciones,
    String motivoCorreccion,
    Identificacion identificacion) {

  public static ActaResponse desde(Acta acta) {
    return desde(acta, false);
  }

  /** Vista del docente: una nota parcial no se le muestra (A4). */
  public static ActaResponse paraDocente(Acta acta) {
    return desde(acta, acta.getEstadoPromedio() != EstadoPromedio.COMPLETO, true);
  }

  private static ActaResponse desde(Acta acta, boolean ocultarNota) {
    return desde(acta, ocultarNota, false);
  }

  private static ActaResponse desde(Acta acta, boolean ocultarNota, boolean paraDocente) {
    return new ActaResponse(
        acta.getIdActa(),
        acta.getDocente().getIdUsuario(),
        acta.getDocente().getNombreCompleto(),
        PeriodoResumenResponse.desde(acta.getPeriodo()),
        acta.getVersion(),
        acta.getEstado(),
        acta.getTipo(),
        ocultarNota ? null : acta.getPromedio(),
        acta.getEstadoPromedio(),
        ocultarNota ? null : CategoriaResponse.desde(acta.getCategoria()),
        acta.getUmbralCompromisos(),
        acta.requiereCompromisos(),
        acta.getCompromisos().stream().map(CompromisoActa::desde).toList(),
        acta.getFirmas().stream().map(Firma::desde).toList(),
        acta.getCodigoVerificacion(),
        acta.getHashContenido(),
        acta.getHashPdf(),
        acta.getFechaFirma(),
        acta.getIdActaAnterior(),
        acta.getObservaciones(),
        acta.getMotivoCorreccion(),
        Identificacion.desde(acta.getDatosCongelados(), paraDocente));
  }

  /**
   * Lo que el acta congeló: identificación del docente, periodo, reunión, pesos y aspectos
   * conversados.
   *
   * @param respuestasEstudiantes cuántos estudiantes respondieron; nulo en la vista del docente
   */
  public record Identificacion(
      String docente,
      String documentoRotulo,
      String documento,
      String cargo,
      String programa,
      String escuela,
      String periodo,
      LocalDate periodoInicio,
      LocalDate periodoFin,
      String evaluador,
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

    static Identificacion desde(DatosCongeladosActa d, boolean paraDocente) {
      return new Identificacion(
          d.docenteNombre(),
          d.documentoRotulo(),
          d.docenteDocumento(),
          d.docenteCargo(),
          d.programaNombre(),
          d.escuelaNombre(),
          d.periodoNombre(),
          d.periodoInicio(),
          d.periodoFin(),
          d.evaluadorCargo(),
          d.reunionFecha(),
          d.reunionHoraInicio(),
          d.reunionHoraFin(),
          d.reunionAsistencia(),
          d.pesoEstudiantes(),
          d.pesoCoordinacion(),
          d.pesoAutoevaluacion(),
          paraDocente ? null : d.respuestasEstudiantes(),
          d.fortalezas(),
          d.aspectosPorMejorar(),
          d.formatoCodigo(),
          d.formatoVersion());
    }
  }

  public record CompromisoActa(Long idCompromiso, String descripcion, LocalDate fechaLimite) {
    static CompromisoActa desde(Compromiso compromiso) {
      return new CompromisoActa(
          compromiso.getIdCompromiso(), compromiso.getDescripcion(), compromiso.getFechaLimite());
    }
  }

  /**
   * @param conTrazo la firma se dibujó en la página y su trazo va en el PDF
   */
  public record Firma(Rol rol, String nombre, LocalDateTime fecha, boolean conTrazo) {
    static Firma desde(FirmaActa firma) {
      return new Firma(
          firma.getRol(), firma.getNombre(), firma.getFecha(), firma.getImagen() != null);
    }
  }
}
