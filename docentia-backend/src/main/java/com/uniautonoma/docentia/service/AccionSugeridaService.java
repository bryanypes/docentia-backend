package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.AccionSugeridaResponse;
import com.uniautonoma.docentia.dto.CategoriaResponse;
import com.uniautonoma.docentia.entity.CategoriaDesempeno;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.PromedioDocente;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.PromedioDocenteRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Traduce cada categoría en la acción que indica la Resolución 074. Solo lee: el sistema informa y
 * la decisión sigue siendo de la institución.
 */
@Service
@Transactional(readOnly = true)
public class AccionSugeridaService {

  static final String ALERTA_MUY_BAJO =
      "Alerta informativa para Vicerrectoría — la institución evalúa el proceso conforme a su"
          + " normativa";

  private final PromedioDocenteRepository promedioRepository;
  private final PeriodoAcademicoRepository periodoRepository;
  private final PeriodoAcademicoService periodoService;

  public AccionSugeridaService(
      PromedioDocenteRepository promedioRepository,
      PeriodoAcademicoRepository periodoRepository,
      PeriodoAcademicoService periodoService) {
    this.promedioRepository = promedioRepository;
    this.periodoRepository = periodoRepository;
    this.periodoService = periodoService;
  }

  /**
   * Lista las acciones de un periodo dentro del alcance del usuario.
   *
   * @param idPeriodo periodo consultado, o nulo para el activo
   * @param sesion coordinador (su facultad) o vicerrector (toda la institución)
   * @return acciones, de la nota más baja a la más alta
   */
  public List<AccionSugeridaResponse> listar(Long idPeriodo, UsuarioSesion sesion) {
    PeriodoAcademico periodo =
        idPeriodo == null
            ? periodoService.exigirPeriodoActivo()
            : periodoRepository
                .findById(idPeriodo)
                .orElseThrow(
                    () -> new RecursoNoEncontradoException("Periodo académico no encontrado"));
    Long idFacultad = sesion.esCoordinador() ? sesion.idFacultad() : null;
    return promedioRepository.listarClasificados(periodo.getIdPeriodo(), idFacultad).stream()
        .map(AccionSugeridaService::describir)
        .toList();
  }

  private static AccionSugeridaResponse describir(PromedioDocente promedio) {
    CategoriaDesempeno categoria = promedio.getCategoria();
    return new AccionSugeridaResponse(
        promedio.getDocente().getIdUsuario(),
        promedio.getDocente().getNombreCompleto(),
        promedio.getDocente().getPrograma().getNombre(),
        promedio.getEstado(),
        CategoriaResponse.desde(categoria),
        categoria == CategoriaDesempeno.MUY_BAJO
            ? ALERTA_MUY_BAJO
            : categoria.getAccionInstitucional());
  }
}
