package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.CitaProgramadaResponse;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.CitaRepository;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Todas las citas agendadas del periodo para la administración: coordinación con sus docentes y
 * vicerrectoría con los coordinadores. Solo lectura; no muestra notas.
 */
@Service
@Transactional(readOnly = true)
public class AgendaAdministracionService {

  private final CitaRepository citaRepository;
  private final PeriodoAcademicoRepository periodoRepository;
  private final PeriodoAcademicoService periodoService;

  public AgendaAdministracionService(
      CitaRepository citaRepository,
      PeriodoAcademicoRepository periodoRepository,
      PeriodoAcademicoService periodoService) {
    this.citaRepository = citaRepository;
    this.periodoRepository = periodoRepository;
    this.periodoService = periodoService;
  }

  /**
   * @param idPeriodo periodo consultado; nulo para el activo
   */
  public List<CitaProgramadaResponse> citasProgramadas(Long idPeriodo) {
    PeriodoAcademico periodo =
        idPeriodo == null
            ? periodoService.exigirPeriodoActivo()
            : periodoRepository
                .findById(idPeriodo)
                .orElseThrow(
                    () -> new RecursoNoEncontradoException("Periodo académico no encontrado"));
    return citaRepository.buscarActivasDelPeriodo(periodo.getIdPeriodo()).stream()
        .map(CitaProgramadaResponse::desde)
        .toList();
  }
}
