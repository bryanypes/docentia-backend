package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.PeriodoResumenResponse;
import com.uniautonoma.docentia.dto.SeguimientoDocentesResponse;
import com.uniautonoma.docentia.dto.SeguimientoDocentesResponse.DocenteConPromedio;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.security.UsuarioSesion;
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

  public SeguimientoDocentesService(
      DocenteService docenteService,
      PeriodoAcademicoService periodoService,
      PromedioService promedioService) {
    this.docenteService = docenteService;
    this.periodoService = periodoService;
    this.promedioService = promedioService;
  }

  /**
   * Lista los docentes que el usuario alcanza a ver con su promedio guardado. Solo trae el promedio
   * ponderado y las fuentes pendientes, nunca las notas por fuente.
   *
   * @param idFacultad facultad por la que filtra el vicerrector; se ignora para el coordinador
   * @param sesion usuario que consulta
   * @return periodo activo y docentes; sin periodo activo, ambos sin promedio
   */
  public SeguimientoDocentesResponse listar(Long idFacultad, UsuarioSesion sesion) {
    Optional<PeriodoAcademico> periodo = periodoService.buscarPeriodoActivo();
    return new SeguimientoDocentesResponse(
        periodo.map(PeriodoResumenResponse::desde).orElse(null),
        docenteService.listarDocentes(idFacultad, sesion).stream()
            .map(
                docente ->
                    new DocenteConPromedio(
                        docente,
                        periodo
                            .map(
                                activo ->
                                    promedioService.obtenerPromedio(docente.idDocente(), activo))
                            .orElse(null)))
            .toList());
  }
}
