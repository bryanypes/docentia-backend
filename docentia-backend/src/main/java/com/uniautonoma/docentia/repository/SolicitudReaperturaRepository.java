package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.EstadoSolicitudReapertura;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.SolicitudReapertura;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Solicitudes para corregir formularios ya enviados. */
public interface SolicitudReaperturaRepository extends JpaRepository<SolicitudReapertura, Long> {

  boolean existsByFormularioIdAndEstadoIn(
      Long idFormulario, Collection<EstadoSolicitudReapertura> estados);

  @EntityGraph(
      attributePaths = {
        "formulario",
        "formulario.docente",
        "formulario.docente.facultad",
        "formulario.docente.programa",
        "formulario.docente.programa.facultad",
        "formulario.periodo",
        "solicitante",
        "aprobador",
        "administrador"
      })
  List<SolicitudReapertura> findBySolicitanteIdUsuarioOrderByFechaSolicitudDesc(Long idUsuario);

  @EntityGraph(
      attributePaths = {
        "formulario",
        "formulario.docente",
        "formulario.docente.facultad",
        "formulario.docente.programa",
        "formulario.docente.programa.facultad",
        "formulario.periodo",
        "solicitante",
        "aprobador",
        "administrador"
      })
  List<SolicitudReapertura> findByRolAprobadorOrderByFechaSolicitudDesc(Rol rol);

  @EntityGraph(
      attributePaths = {
        "formulario",
        "formulario.docente",
        "formulario.docente.facultad",
        "formulario.docente.programa",
        "formulario.docente.programa.facultad",
        "formulario.periodo",
        "solicitante",
        "aprobador",
        "administrador"
      })
  List<SolicitudReapertura> findAllByOrderByFechaSolicitudDesc();
}
