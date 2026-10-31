package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.EstadoHallazgo;
import com.uniautonoma.docentia.entity.VersionHallazgo;
import java.time.LocalDateTime;

/** Una versión de un hallazgo. */
public record VersionHallazgoResponse(
    int version,
    String accion,
    String texto,
    EstadoHallazgo estado,
    String modelo,
    String versionInstruccion,
    String nombreUsuario,
    String motivo,
    LocalDateTime fecha) {

  public static VersionHallazgoResponse desde(VersionHallazgo version) {
    return new VersionHallazgoResponse(
        version.getVersion(),
        version.getAccion().name(),
        version.getTexto(),
        version.getEstado(),
        version.getModelo(),
        version.getVersionInstruccion(),
        version.getUsuario() == null ? null : version.getUsuario().getNombreCompleto(),
        version.getMotivo(),
        version.getFecha());
  }
}
