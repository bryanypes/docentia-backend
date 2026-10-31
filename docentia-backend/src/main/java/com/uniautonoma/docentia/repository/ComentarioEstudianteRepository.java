package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.ComentarioEstudiante;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Acceso a los comentarios de estudiantes cargados desde el CSV. */
public interface ComentarioEstudianteRepository extends JpaRepository<ComentarioEstudiante, Long> {

  /**
   * Inserta el comentario salvo que ya exista con la misma clave de negocio (docente, periodo y
   * hash). Es seguro ante dos cargas simultáneas del mismo archivo.
   *
   * @return 1 si se insertó, 0 si ya estaba
   */
  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
          insert into comentarios_estudiantes
              (id_docente, id_periodo, texto, hash_texto, id_importacion)
          values (:idDocente, :idPeriodo, :texto, :hashTexto, :idImportacion)
          on conflict (id_docente, id_periodo, hash_texto) do nothing
          """,
      nativeQuery = true)
  int insertarSiNoExiste(
      Long idDocente, Long idPeriodo, String texto, String hashTexto, Long idImportacion);

  /**
   * Lista los comentarios de un docente en un periodo.
   *
   * @return comentarios en el orden en que se cargaron
   */
  List<ComentarioEstudiante> findByDocenteIdUsuarioAndPeriodoIdPeriodoOrderByIdComentarioAsc(
      Long idDocente, Long idPeriodo);

  long countByDocenteIdUsuarioAndPeriodoIdPeriodo(Long idDocente, Long idPeriodo);
}
