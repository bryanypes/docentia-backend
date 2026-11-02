package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.dto.FormularioResponse.OpcionEscala;
import java.util.List;

/** Estructura del formulario de heteroevaluación. No lleva respuestas: ni las propias. */
public record FormularioEstudianteResponse(
    Long idAsignacion,
    String curso,
    String grupo,
    String docente,
    String estado,
    boolean ventanaAbierta,
    int total,
    String aviso,
    List<OpcionEscala> escala,
    List<Criterio> criterios) {

  public record Criterio(String codigo, String nombre, List<Descriptor> descriptores) {}

  public record Descriptor(String codigo, String texto) {}
}
