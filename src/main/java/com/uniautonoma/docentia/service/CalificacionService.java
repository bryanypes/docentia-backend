package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.CalificacionRequest;
import com.uniautonoma.docentia.dto.CalificacionResponse;
import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Registro y consulta de la calificación que coordinación le da a un docente. */
@Service
@Transactional
public class CalificacionService {

  private final CalificacionRepository calificacionRepository;
  private final UsuarioRepository usuarioRepository;
  private final DocenteService docenteService;
  private final PeriodoAcademicoService periodoService;

  public CalificacionService(
      CalificacionRepository calificacionRepository,
      UsuarioRepository usuarioRepository,
      DocenteService docenteService,
      PeriodoAcademicoService periodoService) {
    this.calificacionRepository = calificacionRepository;
    this.usuarioRepository = usuarioRepository;
    this.docenteService = docenteService;
    this.periodoService = periodoService;
  }

  /**
   * Guarda la calificación de coordinación para un docente en el periodo activo. Si ya existía, la
   * reemplaza.
   *
   * @param idDocente identificador del docente calificado
   * @param solicitud notas de los cinco aspectos y observación opcional
   * @param sesion coordinador que califica
   * @return la calificación guardada
   * @throws RecursoNoEncontradoException si el docente no existe o es de otra facultad
   * @throws ReglaNegocioException si no hay un periodo académico activo
   */
  public CalificacionResponse calificarDocente(
      Long idDocente, CalificacionRequest solicitud, UsuarioSesion sesion) {
    Usuario docente = docenteService.buscarDocenteVisible(idDocente, sesion);
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    Calificacion calificacion =
        calificacionRepository
            .findByDocenteIdUsuarioAndPeriodoIdPeriodoAndFuente(
                idDocente, periodo.getIdPeriodo(), FuenteEvaluacion.COORDINACION)
            .orElseGet(() -> new Calificacion(docente, periodo, FuenteEvaluacion.COORDINACION));
    String observacion =
        StringUtils.hasText(solicitud.observacion()) ? solicitud.observacion().trim() : null;
    calificacion.registrar(
        solicitud.aNotasAspectos(),
        observacion,
        usuarioRepository.getReferenceById(sesion.idUsuario()));
    return CalificacionResponse.desde(calificacionRepository.saveAndFlush(calificacion));
  }

  /**
   * Consulta la calificación de coordinación de un docente en el periodo activo.
   *
   * @param idDocente identificador del docente
   * @param sesion coordinador que consulta
   * @return la calificación, o vacío si aún no se ha registrado o no hay periodo activo
   * @throws RecursoNoEncontradoException si el docente no existe o es de otra facultad
   */
  @Transactional(readOnly = true)
  public Optional<CalificacionResponse> obtenerCalificacion(Long idDocente, UsuarioSesion sesion) {
    docenteService.buscarDocenteVisible(idDocente, sesion);
    return periodoService
        .buscarPeriodoActivo()
        .flatMap(
            periodo ->
                calificacionRepository.findByDocenteIdUsuarioAndPeriodoIdPeriodoAndFuente(
                    idDocente, periodo.getIdPeriodo(), FuenteEvaluacion.COORDINACION))
        .map(CalificacionResponse::desde);
  }
}
