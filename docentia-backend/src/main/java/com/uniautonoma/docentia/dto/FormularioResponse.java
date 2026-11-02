package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.EstadoFormulario;
import com.uniautonoma.docentia.entity.TipoFormulario;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Estructura del instrumento con las respuestas propias.
 *
 * @param estado nulo si aún no se ha guardado nada
 * @param ventanaAbierta si hoy se puede editar y enviar
 */
public record FormularioResponse(
    PeriodoResumenResponse periodo,
    TipoFormulario tipo,
    Long idDocente,
    String nombreDocente,
    EstadoFormulario estado,
    String instrumentoVersion,
    LocalDateTime enviadaEn,
    boolean ventanaAbierta,
    int respondidos,
    int total,
    List<OpcionEscala> escala,
    List<CriterioFormulario> criterios) {

  public record OpcionEscala(int valor, String etiqueta) {}

  public record CriterioFormulario(
      String codigo, String nombre, String comentario, List<DescriptorFormulario> descriptores) {}

  public record DescriptorFormulario(String codigo, String texto, Integer valor) {}
}
