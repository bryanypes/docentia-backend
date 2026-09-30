package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.DocenteResponse;
import com.uniautonoma.docentia.dto.ExpedientePropioResponse;
import com.uniautonoma.docentia.dto.PeriodoResumenResponse;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Arma el expediente de evaluación de un docente según quién lo consulta. */
@Service
@Transactional(readOnly = true)
public class ExpedienteService {

  private final UsuarioRepository usuarioRepository;
  private final PeriodoAcademicoService periodoService;
  private final PromedioService promedioService;

  public ExpedienteService(
      UsuarioRepository usuarioRepository,
      PeriodoAcademicoService periodoService,
      PromedioService promedioService) {
    this.usuarioRepository = usuarioRepository;
    this.periodoService = periodoService;
    this.promedioService = promedioService;
  }

  /**
   * Obtiene el expediente del docente autenticado, sin las notas individuales de cada fuente.
   *
   * @param sesion docente que consulta su propia información
   * @return datos del docente, periodo activo y su promedio ponderado o el aviso de incompleto
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
        periodoActivo
            .map(
                periodo -> promedioService.calcularPromedioDocente(docente.getIdUsuario(), periodo))
            .orElse(null));
  }
}
