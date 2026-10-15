package com.uniautonoma.docentia.service.analisis;

import com.uniautonoma.docentia.entity.ComentarioFiltrado;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import com.uniautonoma.docentia.repository.ComentarioEstudianteRepository;
import com.uniautonoma.docentia.repository.ComentarioFiltradoRepository;
import com.uniautonoma.docentia.service.analisis.FiltroComentarios.Exclusion;
import com.uniautonoma.docentia.service.analisis.ModeloIA.ComentarioAnonimo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Arma la lista de comentarios que se envía al modelo: primero el filtro (los excluidos quedan
 * registrados y no se envían), después la anonimización de los que pasan.
 */
@Component
public class PreparadorComentarios {

  private final ProveedorComentarios proveedorComentarios;
  private final FiltroComentarios filtro;
  private final Anonimizador anonimizador;
  private final ComentarioFiltradoRepository filtradoRepository;
  private final ComentarioEstudianteRepository comentarioRepository;

  public PreparadorComentarios(
      ProveedorComentarios proveedorComentarios,
      FiltroComentarios filtro,
      Anonimizador anonimizador,
      ComentarioFiltradoRepository filtradoRepository,
      ComentarioEstudianteRepository comentarioRepository) {
    this.proveedorComentarios = proveedorComentarios;
    this.filtro = filtro;
    this.anonimizador = anonimizador;
    this.filtradoRepository = filtradoRepository;
    this.comentarioRepository = comentarioRepository;
  }

  /**
   * Filtra y anonimiza los comentarios del docente de la solicitud. Debe llamarse dentro de una
   * transacción: guarda los comentarios excluidos.
   *
   * @return los comentarios que se envían y cuántos se excluyeron
   */
  public Preparados preparar(SolicitudAnalisis solicitud) {
    List<ComentarioAnonimo> enviados = new ArrayList<>();
    int filtrados = 0;
    for (ComentarioFuente comentario :
        proveedorComentarios.comentarios(
            solicitud.getDocente().getIdUsuario(), solicitud.getPeriodo().getIdPeriodo())) {
      Optional<Exclusion> exclusion = filtro.evaluar(comentario.texto());
      if (exclusion.isPresent()) {
        filtradoRepository.save(
            new ComentarioFiltrado(
                solicitud,
                comentarioRepository.getReferenceById(comentario.idComentario()),
                exclusion.get().categoria(),
                exclusion.get().regla()));
        filtrados++;
      } else {
        enviados.add(
            new ComentarioAnonimo(
                comentario.idComentario(), anonimizador.anonimizar(comentario.texto())));
      }
    }
    return new Preparados(List.copyOf(enviados), filtrados);
  }

  /** Comentarios listos para el modelo y cuántos se quedaron por fuera por el filtro. */
  public record Preparados(List<ComentarioAnonimo> enviados, int filtrados) {}
}
