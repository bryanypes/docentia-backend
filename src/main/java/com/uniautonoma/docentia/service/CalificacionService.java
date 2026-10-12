package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.CalificacionRequest;
import com.uniautonoma.docentia.dto.CalificacionResponse;
import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.NotasAspectos;
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

/**
 * Registro y consulta de las calificaciones de un docente. Cada registro recalcula y guarda su
 * promedio del periodo.
 */
@Service
@Transactional
public class CalificacionService {

  private final CalificacionRepository calificacionRepository;
  private final UsuarioRepository usuarioRepository;
  private final DocenteService docenteService;
  private final PeriodoAcademicoService periodoService;
  private final PromedioService promedioService;

  public CalificacionService(
      CalificacionRepository calificacionRepository,
      UsuarioRepository usuarioRepository,
      DocenteService docenteService,
      PeriodoAcademicoService periodoService,
      PromedioService promedioService) {
    this.calificacionRepository = calificacionRepository;
    this.usuarioRepository = usuarioRepository;
    this.docenteService = docenteService;
    this.periodoService = periodoService;
    this.promedioService = promedioService;
  }

  /**
   * Guarda la calificación de una fuente para un docente en el periodo activo. Si esa fuente ya
   * había calificado, la reemplaza.
   *
   * @param idDocente identificador del docente calificado
   * @param fuente fuente a nombre de la cual se califica
   * @param solicitud notas de los cinco aspectos y observación opcional
   * @param sesion usuario que califica; su alcance decide qué docentes puede calificar
   * @return la calificación guardada
   * @throws RecursoNoEncontradoException si el docente no existe o está fuera del alcance
   * @throws ReglaNegocioException si no hay un periodo académico activo
   */
  public CalificacionResponse calificarDocente(
      Long idDocente,
      FuenteEvaluacion fuente,
      CalificacionRequest solicitud,
      UsuarioSesion sesion) {
    Usuario docente = docenteService.buscarDocenteVisible(idDocente, sesion);
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    Calificacion calificacion =
        calificacionRepository
            .findByDocenteIdUsuarioAndPeriodoIdPeriodoAndFuente(
                idDocente, periodo.getIdPeriodo(), fuente)
            .orElseGet(() -> new Calificacion(docente, periodo, fuente));
    String observacion =
        StringUtils.hasText(solicitud.observacion()) ? solicitud.observacion().trim() : null;
    calificacion.registrar(
        solicitud.aNotasAspectos(),
        observacion,
        usuarioRepository.getReferenceById(sesion.idUsuario()));
    CalificacionResponse respuesta =
        CalificacionResponse.desde(calificacionRepository.saveAndFlush(calificacion));
    promedioService.recalcular(idDocente, periodo.getIdPeriodo());
    return respuesta;
  }

  /**
   * Guarda la nota consolidada de los estudiantes para un docente en un periodo. Es la entrada que
   * usará la importación desde Campus; si ya había una nota de estudiantes, la reemplaza.
   *
   * @param docente docente evaluado
   * @param periodo periodo al que corresponde la evaluación
   * @param notas promedio de los cinco aspectos que dieron los estudiantes
   * @return la calificación guardada
   */
  public CalificacionResponse registrarCalificacionEstudiantes(
      Usuario docente, PeriodoAcademico periodo, NotasAspectos notas) {
    Calificacion calificacion =
        calificacionRepository
            .findByDocenteIdUsuarioAndPeriodoIdPeriodoAndFuente(
                docente.getIdUsuario(), periodo.getIdPeriodo(), FuenteEvaluacion.ESTUDIANTES)
            .orElseGet(() -> new Calificacion(docente, periodo, FuenteEvaluacion.ESTUDIANTES));
    calificacion.registrar(notas, null, null);
    CalificacionResponse respuesta =
        CalificacionResponse.desde(calificacionRepository.saveAndFlush(calificacion));
    promedioService.recalcular(docente.getIdUsuario(), periodo.getIdPeriodo());
    return respuesta;
  }

  /**
   * Consulta la calificación que una fuente le dio a un docente en el periodo activo.
   *
   * @param idDocente identificador del docente
   * @param fuente fuente consultada
   * @param sesion usuario que consulta
   * @return la calificación, o vacío si aún no se ha registrado o no hay periodo activo
   * @throws RecursoNoEncontradoException si el docente no existe o está fuera del alcance
   */
  @Transactional(readOnly = true)
  public Optional<CalificacionResponse> obtenerCalificacion(
      Long idDocente, FuenteEvaluacion fuente, UsuarioSesion sesion) {
    docenteService.buscarDocenteVisible(idDocente, sesion);
    return periodoService
        .buscarPeriodoActivo()
        .flatMap(
            periodo ->
                calificacionRepository.findByDocenteIdUsuarioAndPeriodoIdPeriodoAndFuente(
                    idDocente, periodo.getIdPeriodo(), fuente))
        .map(CalificacionResponse::desde);
  }
}
