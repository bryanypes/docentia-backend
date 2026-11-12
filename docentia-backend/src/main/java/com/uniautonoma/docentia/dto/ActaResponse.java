package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Acta;
import com.uniautonoma.docentia.entity.Compromiso;
import com.uniautonoma.docentia.entity.EstadoActa;
import com.uniautonoma.docentia.entity.FirmaActa;
import com.uniautonoma.docentia.entity.Rol;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Acta sin notas por fuente: la consulta también el docente (A4). */
public record ActaResponse(
    Long idActa,
    Long idDocente,
    String nombreDocente,
    PeriodoResumenResponse periodo,
    int version,
    EstadoActa estado,
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
    String motivoCorreccion) {

  public static ActaResponse desde(Acta acta) {
    return desde(acta, false);
  }

  /** Vista del docente: una nota parcial no se le muestra (A4). */
  public static ActaResponse paraDocente(Acta acta) {
    return desde(acta, acta.getEstadoPromedio() != EstadoPromedio.COMPLETO);
  }

  private static ActaResponse desde(Acta acta, boolean ocultarNota) {
    return new ActaResponse(
        acta.getIdActa(),
        acta.getDocente().getIdUsuario(),
        acta.getDocente().getNombreCompleto(),
        PeriodoResumenResponse.desde(acta.getPeriodo()),
        acta.getVersion(),
        acta.getEstado(),
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
        acta.getMotivoCorreccion());
  }

  public record CompromisoActa(Long idCompromiso, String descripcion, LocalDate fechaLimite) {
    static CompromisoActa desde(Compromiso compromiso) {
      return new CompromisoActa(
          compromiso.getIdCompromiso(), compromiso.getDescripcion(), compromiso.getFechaLimite());
    }
  }

  public record Firma(Rol rol, String nombre, LocalDateTime fecha) {
    static Firma desde(FirmaActa firma) {
      return new Firma(firma.getRol(), firma.getNombre(), firma.getFecha());
    }
  }
}
