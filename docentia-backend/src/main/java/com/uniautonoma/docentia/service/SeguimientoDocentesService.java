package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.PeriodoResumenResponse;
import com.uniautonoma.docentia.dto.PromedioPonderadoResponse;
import com.uniautonoma.docentia.dto.SeguimientoDocentesResponse;
import com.uniautonoma.docentia.dto.SeguimientoDocentesResponse.DocenteConPromedio;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lista de docentes con su promedio del periodo activo para el tablero de seguimiento. */
@Service
@Transactional(readOnly = true)
public class SeguimientoDocentesService {

  private final DocenteService docenteService;
  private final PeriodoAcademicoService periodoService;
  private final PromedioService promedioService;
  private final PeriodoAcademicoRepository periodoRepository;

  public SeguimientoDocentesService(
      DocenteService docenteService,
      PeriodoAcademicoService periodoService,
      PromedioService promedioService,
      PeriodoAcademicoRepository periodoRepository) {
    this.docenteService = docenteService;
    this.periodoService = periodoService;
    this.promedioService = promedioService;
    this.periodoRepository = periodoRepository;
  }

  /**
   * Lista los docentes que el usuario alcanza a ver con su promedio guardado. Solo trae el promedio
   * ponderado y las fuentes pendientes, nunca las notas por fuente.
   *
   * @param idFacultad facultad por la que filtra el vicerrector; se ignora para el coordinador
   * @param idPeriodo periodo consultado, o nulo para el activo
   * @param idPrograma programa (carrera) por el que se filtra, o nulo para todos los del alcance
   * @param sesion usuario que consulta
   * @return periodo y docentes; sin periodo (ni pedido ni activo), ambos sin promedio
   * @throws RecursoNoEncontradoException si el periodo pedido no existe
   */
  public SeguimientoDocentesResponse listar(
      Long idFacultad, Long idPeriodo, Long idPrograma, UsuarioSesion sesion) {
    Optional<PeriodoAcademico> periodo =
        idPeriodo == null
            ? periodoService.buscarPeriodoActivo()
            : Optional.of(
                periodoRepository
                    .findById(idPeriodo)
                    .orElseThrow(
                        () -> new RecursoNoEncontradoException("Periodo académico no encontrado")));
    Map<Long, PromedioPonderadoResponse> promedios =
        periodo
            .map(activo -> promedioService.obtenerPromedios(activo.getIdPeriodo()))
            .orElse(Map.of());
    return new SeguimientoDocentesResponse(
        periodo.map(PeriodoResumenResponse::desde).orElse(null),
        docenteService.listarDocentes(idFacultad, sesion).stream()
            .filter(docente -> idPrograma == null || idPrograma.equals(docente.idPrograma()))
            .map(
                docente ->
                    new DocenteConPromedio(
                        docente,
                        periodo.isEmpty()
                            ? null
                            : promedios.getOrDefault(
                                docente.idDocente(), PromedioPonderadoResponse.sinNota())))
            .toList());
  }
}
