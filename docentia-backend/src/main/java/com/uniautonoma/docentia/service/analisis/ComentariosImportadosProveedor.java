package com.uniautonoma.docentia.service.analisis;

import com.uniautonoma.docentia.repository.ComentarioEstudianteRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Comentarios cargados con el CSV de evaluación de estudiantes (C4).
 *
 * <p>Encuesta cerrada: respuesta de un estudiante con comentario abierto que llegó en una carga de
 * CSV exitosa para ese docente y periodo. El CSV de Power BI se exporta cuando la encuesta ya
 * cerró, así que todo lo importado cuenta como cerrado. Las filas que solo traen notas no cuentan:
 * sin comentario no hay nada que analizar.
 */
@Component
public class ComentariosImportadosProveedor implements ProveedorComentarios {

  private final ComentarioEstudianteRepository comentarioRepository;

  public ComentariosImportadosProveedor(ComentarioEstudianteRepository comentarioRepository) {
    this.comentarioRepository = comentarioRepository;
  }

  @Override
  public int contarEncuestasCerradas(Long idDocente, Long idPeriodo) {
    return (int)
        comentarioRepository.countByDocenteIdUsuarioAndPeriodoIdPeriodo(idDocente, idPeriodo);
  }

  @Override
  public List<ComentarioFuente> comentarios(Long idDocente, Long idPeriodo) {
    return comentarioRepository
        .findByDocenteIdUsuarioAndPeriodoIdPeriodoOrderByIdComentarioAsc(idDocente, idPeriodo)
        .stream()
        .map(
            comentario -> new ComentarioFuente(comentario.getIdComentario(), comentario.getTexto()))
        .toList();
  }
}
