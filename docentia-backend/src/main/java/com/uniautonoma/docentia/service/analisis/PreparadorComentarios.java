package com.uniautonoma.docentia.service.analisis;

import com.uniautonoma.docentia.entity.ComentarioFiltrado;
import com.uniautonoma.docentia.entity.FuenteComentarios;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import com.uniautonoma.docentia.repository.ComentarioEstudianteRepository;
import com.uniautonoma.docentia.repository.ComentarioFiltradoRepository;
import com.uniautonoma.docentia.service.analisis.ModeloIA.ComentarioAnonimo;
import com.uniautonoma.docentia.service.analisis.local.FiltroCalidad.Evaluado;
import com.uniautonoma.docentia.service.analisis.local.PipelineAnalisis;
import com.uniautonoma.docentia.service.analisis.local.PipelineAnalisis.FiltradoContenido;
import com.uniautonoma.docentia.service.analisis.local.PipelineAnalisis.Preparacion;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Arma la lista de comentarios que se envía al modelo con el pipeline común: filtro de calidad
 * (vacíos, muy cortos, duplicados, instrucciones), filtro de contenido (los excluidos del CSV
 * quedan registrados como hasta ahora) y anonimización, con el nombre del docente incluido.
 */
@Component
public class PreparadorComentarios {

  private final ProveedoresComentarios proveedores;
  private final PipelineAnalisis pipeline;
  private final ComentarioFiltradoRepository filtradoRepository;
  private final ComentarioEstudianteRepository comentarioRepository;

  public PreparadorComentarios(
      ProveedoresComentarios proveedores,
      PipelineAnalisis pipeline,
      ComentarioFiltradoRepository filtradoRepository,
      ComentarioEstudianteRepository comentarioRepository) {
    this.proveedores = proveedores;
    this.pipeline = pipeline;
    this.filtradoRepository = filtradoRepository;
    this.comentarioRepository = comentarioRepository;
  }

  /**
   * Filtra y anonimiza los comentarios del docente de la solicitud. Debe llamarse dentro de una
   * transacción: guarda los comentarios excluidos por contenido cuando vienen del CSV.
   *
   * @param maximo comentarios que se conservan como máximo, en el orden de la huella
   * @return los comentarios que se envían, cuántos se excluyeron y la preparación completa
   */
  public Preparados preparar(SolicitudAnalisis solicitud, int maximo) {
    List<ComentarioFuente> fuente =
        proveedores
            .para(solicitud.getFuente())
            .comentarios(
                solicitud.getDocente().getIdUsuario(), solicitud.getPeriodo().getIdPeriodo());
    Preparacion preparacion =
        pipeline.preparar(
            fuente.stream().map(c -> new Evaluado(c.idComentario(), c.texto())).toList(),
            solicitud.getDocente().getNombreCompleto());
    boolean recortado = preparacion.utiles().size() > maximo;
    if (recortado) {
      preparacion =
          new Preparacion(
              preparacion.utiles().subList(0, maximo),
              preparacion.excluidos(),
              preparacion.filtrados(),
              preparacion.truncados());
    }
    // Las respuestas del formulario no tienen un id que se pueda guardar sin romper el anonimato:
    // de ellas solo queda el conteo por motivo dentro del resultado.
    if (solicitud.getFuente() == FuenteComentarios.CSV) {
      for (FiltradoContenido filtrado : preparacion.filtrados()) {
        filtradoRepository.save(
            new ComentarioFiltrado(
                solicitud,
                comentarioRepository.getReferenceById(filtrado.id()),
                filtrado.exclusion().categoria(),
                filtrado.exclusion().regla()));
      }
    }
    // El campo histórico cuenta solo los excluidos por contenido (E2); el desglose completo por
    // motivo va en el resultado estructurado.
    return new Preparados(
        preparacion.comoAnonimos(),
        preparacion.filtrados().size(),
        preparacion,
        fuente.size(),
        recortado);
  }

  /**
   * Comentarios listos para el modelo.
   *
   * @param enviados anonimizados, en el orden de la huella
   * @param filtrados excluidos por contenido ofensivo o sobre la persona (E2)
   * @param leidos comentarios no vacíos que entregó la fuente
   * @param recortado si se superó el máximo por corrida
   */
  public record Preparados(
      List<ComentarioAnonimo> enviados,
      int filtrados,
      Preparacion preparacion,
      int leidos,
      boolean recortado) {}
}
