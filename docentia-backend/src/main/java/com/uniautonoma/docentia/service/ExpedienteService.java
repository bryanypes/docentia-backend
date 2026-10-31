package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.CompromisoResponse;
import com.uniautonoma.docentia.dto.DimensionPorFuenteResponse;
import com.uniautonoma.docentia.dto.DocenteResponse;
import com.uniautonoma.docentia.dto.ExpedienteDocenteResponse;
import com.uniautonoma.docentia.dto.ExpedientePropioResponse;
import com.uniautonoma.docentia.dto.NotaFuenteResponse;
import com.uniautonoma.docentia.dto.PeriodoResumenResponse;
import com.uniautonoma.docentia.dto.PromedioHistoricoResponse;
import com.uniautonoma.docentia.dto.PromedioPonderadoResponse;
import com.uniautonoma.docentia.dto.ResultadoDimensionResponse;
import com.uniautonoma.docentia.entity.Dimension;
import com.uniautonoma.docentia.entity.EstadoCompromiso;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.CalificacionRepository;
import com.uniautonoma.docentia.repository.CompromisoRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Arma el expediente de evaluación de un docente según quién lo consulta. */
@Service
@Transactional(readOnly = true)
public class ExpedienteService {

  private static final int DECIMALES_NOTA_FUENTE = 2;

  private final UsuarioRepository usuarioRepository;
  private final CalificacionRepository calificacionRepository;
  private final CompromisoRepository compromisoRepository;
  private final DocenteService docenteService;
  private final PeriodoAcademicoService periodoService;
  private final PromedioService promedioService;

  public ExpedienteService(
      UsuarioRepository usuarioRepository,
      CalificacionRepository calificacionRepository,
      CompromisoRepository compromisoRepository,
      DocenteService docenteService,
      PeriodoAcademicoService periodoService,
      PromedioService promedioService) {
    this.usuarioRepository = usuarioRepository;
    this.calificacionRepository = calificacionRepository;
    this.compromisoRepository = compromisoRepository;
    this.docenteService = docenteService;
    this.periodoService = periodoService;
    this.promedioService = promedioService;
  }

  /**
   * Obtiene el expediente completo de un docente para coordinación o vicerrectoría.
   *
   * @param idDocente identificador del docente
   * @param sesion usuario que consulta; el coordinador solo alcanza docentes de su facultad
   * @return notas por fuente, resultado por dimensión, histórico y compromisos vigentes
   * @throws RecursoNoEncontradoException si el docente no existe o está fuera del alcance
   */
  public ExpedienteDocenteResponse obtenerExpedienteDocente(Long idDocente, UsuarioSesion sesion) {
    Usuario docente = docenteService.buscarDocenteVisible(idDocente, sesion);
    Optional<PeriodoAcademico> periodoActivo = periodoService.buscarPeriodoActivo();
    Optional<EvaluacionPeriodo> evaluacion =
        periodoActivo.map(periodo -> promedioService.cargarEvaluacion(idDocente, periodo));
    return new ExpedienteDocenteResponse(
        DocenteResponse.desde(docente),
        periodoActivo.map(PeriodoResumenResponse::desde).orElse(null),
        periodoActivo
            .map(periodo -> promedioService.obtenerPromedio(idDocente, periodo))
            .orElse(null),
        evaluacion.map(this::listarNotasPorFuente).orElse(List.of()),
        evaluacion.map(this::listarDimensionesPorFuente).orElse(List.of()),
        listarHistorico(idDocente, periodoActivo.map(PeriodoAcademico::getIdPeriodo).orElse(null)),
        listarCompromisosVigentes(idDocente));
  }

  /**
   * Obtiene el expediente del docente autenticado, sin las notas individuales de cada fuente.
   *
   * @param sesion docente que consulta su propia información
   * @return promedio ponderado o aviso de incompleto, resultado por dimensión ya ponderado y
   *     compromisos vigentes
   * @throws RecursoNoEncontradoException si el usuario de la sesión ya no es un docente activo
   */
  public ExpedientePropioResponse obtenerExpedientePropio(UsuarioSesion sesion) {
    Usuario docente =
        usuarioRepository
            .findByIdUsuarioAndRolAndEsActivoTrue(sesion.idUsuario(), Rol.DOCENTE)
            .orElseThrow(() -> new RecursoNoEncontradoException("Docente no encontrado"));
    Optional<PeriodoAcademico> periodoActivo = periodoService.buscarPeriodoActivo();
    Optional<EvaluacionPeriodo> evaluacion =
        periodoActivo.map(
            periodo -> promedioService.cargarEvaluacion(docente.getIdUsuario(), periodo));
    return new ExpedientePropioResponse(
        DocenteResponse.desde(docente),
        periodoActivo.map(PeriodoResumenResponse::desde).orElse(null),
        periodoActivo
            .map(periodo -> promedioService.obtenerPromedio(docente.getIdUsuario(), periodo))
            .orElse(null),
        evaluacion.map(this::listarResultadosPorDimension).orElse(List.of()),
        listarCompromisosVigentes(docente.getIdUsuario()));
  }

  private List<ResultadoDimensionResponse> listarResultadosPorDimension(
      EvaluacionPeriodo evaluacion) {
    return Arrays.stream(Dimension.values())
        .map(
            dimension ->
                ResultadoDimensionResponse.desde(
                    dimension, promedioService.calcularResultadoDimension(evaluacion, dimension)))
        .toList();
  }

  private List<NotaFuenteResponse> listarNotasPorFuente(EvaluacionPeriodo evaluacion) {
    Map<FuenteEvaluacion, BigDecimal> notas = evaluacion.notasDeFuentes();
    return Arrays.stream(FuenteEvaluacion.values())
        .map(
            fuente ->
                new NotaFuenteResponse(
                    fuente, redondear(notas.get(fuente)), evaluacion.pesos().pesoDe(fuente)))
        .toList();
  }

  private List<DimensionPorFuenteResponse> listarDimensionesPorFuente(
      EvaluacionPeriodo evaluacion) {
    return Arrays.stream(Dimension.values())
        .map(
            dimension -> {
              Map<FuenteEvaluacion, BigDecimal> notas = evaluacion.notasDeDimension(dimension);
              PromedioPonderadoResponse resultado =
                  promedioService.calcularResultadoDimension(evaluacion, dimension);
              return new DimensionPorFuenteResponse(
                  dimension,
                  dimension.getNombre(),
                  notas.get(FuenteEvaluacion.ESTUDIANTES),
                  notas.get(FuenteEvaluacion.COORDINACION),
                  notas.get(FuenteEvaluacion.VICERRECTORIA),
                  resultado.estado(),
                  resultado.valor());
            })
        .toList();
  }

  private List<PromedioHistoricoResponse> listarHistorico(Long idDocente, Long idPeriodoActivo) {
    return calificacionRepository.buscarPeriodosCalificados(idDocente).stream()
        .filter(periodo -> !periodo.getIdPeriodo().equals(idPeriodoActivo))
        .map(
            periodo ->
                new PromedioHistoricoResponse(
                    PeriodoResumenResponse.desde(periodo),
                    promedioService.obtenerPromedio(idDocente, periodo)))
        .toList();
  }

  private List<CompromisoResponse> listarCompromisosVigentes(Long idDocente) {
    return compromisoRepository
        .findByDocenteIdUsuarioAndEstadoOrderByFechaLimiteAsc(idDocente, EstadoCompromiso.VIGENTE)
        .stream()
        .map(CompromisoResponse::desde)
        .toList();
  }

  private BigDecimal redondear(BigDecimal nota) {
    return nota == null ? null : nota.setScale(DECIMALES_NOTA_FUENTE, RoundingMode.HALF_UP);
  }
}
