package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.EstadoFormulario;
import com.uniautonoma.docentia.entity.EvaluacionFormulario;
import com.uniautonoma.docentia.entity.TipoFormulario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Formularios respondidos. */
public interface EvaluacionFormularioRepository extends JpaRepository<EvaluacionFormulario, Long> {

  /** Estado de un formulario, sin puntajes. */
  record AvanceFormulario(
      Long idDocente, TipoFormulario tipo, EstadoFormulario estado, Integer respondidos) {}

  Optional<EvaluacionFormulario> findByPeriodoIdPeriodoAndDocenteIdUsuarioAndTipo(
      Long idPeriodo, Long idDocente, TipoFormulario tipo);

  @Query(
      """
      select new com.uniautonoma.docentia.repository.EvaluacionFormularioRepository$AvanceFormulario(
          e.docente.idUsuario, e.tipo, e.estado, size(e.respuestas))
      from EvaluacionFormulario e
      where e.periodo.idPeriodo = :idPeriodo
      """)
  List<AvanceFormulario> leerAvance(Long idPeriodo);
}
