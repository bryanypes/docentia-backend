package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.DocenteResponse;
import com.uniautonoma.docentia.dto.ExpedientePropioResponse;
import com.uniautonoma.docentia.dto.PeriodoResumenResponse;
import com.uniautonoma.docentia.entity.Calificacion;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Arma el expediente de evaluación de un docente según quién lo consulta. */
@Service
@Transactional(readOnly = true)
public class ExpedienteService {

  private final UsuarioRepository usuarioRepository;
  private final CalificacionRepository calificacionRepository;
  private final PeriodoAcademicoService periodoService;

  public ExpedienteService(
      UsuarioRepository usuarioRepository,
      CalificacionRepository calificacionRepository,
      PeriodoAcademicoService periodoService) {
    this.usuarioRepository = usuarioRepository;
    this.calificacionRepository = calificacionRepository;
    this.periodoService = periodoService;
  }

  /**
   * Obtiene el expediente del docente autenticado, sin las notas individuales de cada fuente.
   *
   * @param sesion docente que consulta su propia información
   * @return datos del docente, periodo activo y fuentes que aún no lo han calificado
   * @throws RecursoNoEncontradoException si el usuario de la sesión ya no es un docente activo
   */
  public ExpedientePropioResponse obtenerExpedientePropio(UsuarioSesion sesion) {
    Usuario docente =
        usuarioRepository
            .findByIdUsuarioAndRolAndEsActivoTrue(sesion.idUsuario(), Rol.DOCENTE)
            .orElseThrow(() -> new RecursoNoEncontradoException("Docente no encontrado"));
    Optional<PeriodoAcademico> periodoActivo = periodoService.buscarPeriodoActivo();
    return new ExpedientePropioResponse(
        DocenteResponse.desde(docente),
        periodoActivo.map(PeriodoResumenResponse::desde).orElse(null),
        periodoActivo.map(periodo -> buscarFuentesPendientes(docente, periodo)).orElse(List.of()));
  }

  private List<FuenteEvaluacion> buscarFuentesPendientes(
      Usuario docente, PeriodoAcademico periodo) {
    Set<FuenteEvaluacion> pendientes = EnumSet.allOf(FuenteEvaluacion.class);
    calificacionRepository
        .findByDocenteIdUsuarioAndPeriodoIdPeriodo(docente.getIdUsuario(), periodo.getIdPeriodo())
        .stream()
        .map(Calificacion::getFuente)
        .forEach(pendientes::remove);
    return List.copyOf(pendientes);
  }
}
