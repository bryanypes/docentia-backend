package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.EtapaPeriodoRequest;
import com.uniautonoma.docentia.dto.PeriodoAcademicoRequest;
import com.uniautonoma.docentia.dto.PeriodoAcademicoResponse;
import com.uniautonoma.docentia.dto.VentanaEtapaResponse;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.exception.DatosInvalidosException;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas de negocio de los periodos académicos y sus etapas. */
@Service
@Transactional
public class PeriodoAcademicoService {

  private final PeriodoAcademicoRepository periodoRepository;
  private final VentanaEtapaService ventanaEtapaService;

  public PeriodoAcademicoService(
      PeriodoAcademicoRepository periodoRepository, VentanaEtapaService ventanaEtapaService) {
    this.periodoRepository = periodoRepository;
    this.ventanaEtapaService = ventanaEtapaService;
  }

  /**
   * Describe hoy las cuatro etapas del periodo activo. Las etapas se cargan bajo demanda, así que
   * el periodo y sus etapas se leen dentro de la misma transacción.
   *
   * @return una entrada por etapa, en el orden del proceso
   * @throws ReglaNegocioException si no hay un periodo activo
   */
  @Transactional(readOnly = true)
  public List<VentanaEtapaResponse> describirVentanasActivas() {
    return ventanaEtapaService.describirVentanas(exigirPeriodoActivo());
  }

  /**
   * Lista los periodos académicos.
   *
   * @return periodos del más reciente al más antiguo, con sus etapas
   */
  @Transactional(readOnly = true)
  public List<PeriodoAcademicoResponse> listarPeriodos() {
    return periodoRepository.findAllByOrderByFechaInicioDesc().stream()
        .map(PeriodoAcademicoResponse::desde)
        .toList();
  }

  /**
   * Obtiene un periodo académico.
   *
   * @param idPeriodo identificador del periodo
   * @return el periodo solicitado
   * @throws RecursoNoEncontradoException si el periodo no existe
   */
  @Transactional(readOnly = true)
  public PeriodoAcademicoResponse obtenerPeriodo(Long idPeriodo) {
    return PeriodoAcademicoResponse.desde(buscarPeriodo(idPeriodo));
  }

  /**
   * Obtiene el periodo que está activo.
   *
   * @return el periodo activo
   * @throws RecursoNoEncontradoException si no hay un periodo activo
   */
  @Transactional(readOnly = true)
  public PeriodoAcademicoResponse obtenerPeriodoActivo() {
    return periodoRepository
        .findByEsActivoTrue()
        .map(PeriodoAcademicoResponse::desde)
        .orElseThrow(() -> new RecursoNoEncontradoException("No hay un periodo académico activo"));
  }

  /**
   * Busca la entidad del periodo activo.
   *
   * @return el periodo activo, o vacío si no hay ninguno
   */
  @Transactional(readOnly = true)
  public Optional<PeriodoAcademico> buscarPeriodoActivo() {
    return periodoRepository.findByEsActivoTrue();
  }

  /**
   * Obtiene el periodo activo para una operación que no puede hacerse sin él.
   *
   * @return el periodo activo
   * @throws ReglaNegocioException si no hay un periodo activo
   */
  @Transactional(readOnly = true)
  public PeriodoAcademico exigirPeriodoActivo() {
    return buscarPeriodoActivo()
        .orElseThrow(
            () ->
                new ReglaNegocioException(
                    "No hay un periodo académico activo; la vicerrectoría debe activar uno"));
  }

  /**
   * Registra un periodo académico. El periodo nace inactivo; se activa de forma explícita.
   *
   * @param solicitud nombre, fechas y etapas del periodo
   * @return el periodo creado
   * @throws DatosInvalidosException si las fechas del periodo o de sus etapas son incoherentes
   * @throws ReglaNegocioException si ya existe un periodo con ese nombre
   */
  public PeriodoAcademicoResponse crearPeriodo(PeriodoAcademicoRequest solicitud) {
    validarFechas(solicitud);
    String nombre = solicitud.nombre().trim();
    validarNombreDisponible(nombre, null);
    PeriodoAcademico periodo =
        new PeriodoAcademico(nombre, solicitud.fechaInicio(), solicitud.fechaFin());
    aplicarEtapas(periodo, solicitud.etapas());
    return PeriodoAcademicoResponse.desde(periodoRepository.save(periodo));
  }

  /**
   * Actualiza los datos y las etapas de un periodo.
   *
   * @param idPeriodo identificador del periodo
   * @param solicitud datos nuevos; las etapas que no vengan se retiran
   * @return el periodo actualizado
   * @throws RecursoNoEncontradoException si el periodo no existe
   * @throws DatosInvalidosException si las fechas del periodo o de sus etapas son incoherentes
   * @throws ReglaNegocioException si el nombre ya lo usa otro periodo
   */
  public PeriodoAcademicoResponse actualizarPeriodo(
      Long idPeriodo, PeriodoAcademicoRequest solicitud) {
    PeriodoAcademico periodo = buscarPeriodo(idPeriodo);
    validarFechas(solicitud);
    String nombre = solicitud.nombre().trim();
    validarNombreDisponible(nombre, idPeriodo);
    periodo.setNombre(nombre);
    periodo.setFechaInicio(solicitud.fechaInicio());
    periodo.setFechaFin(solicitud.fechaFin());
    aplicarEtapas(periodo, solicitud.etapas());
    return PeriodoAcademicoResponse.desde(periodo);
  }

  /**
   * Activa un periodo y desactiva el que estuviera activo, de modo que solo quede uno.
   *
   * @param idPeriodo identificador del periodo que pasa a ser el activo
   * @return el periodo ya activo
   * @throws RecursoNoEncontradoException si el periodo no existe
   */
  public PeriodoAcademicoResponse activarPeriodo(Long idPeriodo) {
    periodoRepository.desactivarPeriodoActivo();
    PeriodoAcademico periodo = buscarPeriodo(idPeriodo);
    periodo.setEsActivo(true);
    return PeriodoAcademicoResponse.desde(periodo);
  }

  /**
   * Desactiva un periodo, dejando el sistema sin periodo activo si era el vigente.
   *
   * @param idPeriodo identificador del periodo
   * @throws RecursoNoEncontradoException si el periodo no existe
   */
  public void desactivarPeriodo(Long idPeriodo) {
    buscarPeriodo(idPeriodo).setEsActivo(false);
  }

  private PeriodoAcademico buscarPeriodo(Long idPeriodo) {
    return periodoRepository
        .findById(idPeriodo)
        .orElseThrow(() -> new RecursoNoEncontradoException("Periodo académico no encontrado"));
  }

  private void validarNombreDisponible(String nombre, Long idPeriodoActual) {
    periodoRepository
        .findByNombre(nombre)
        .filter(existente -> !existente.getIdPeriodo().equals(idPeriodoActual))
        .ifPresent(
            existente -> {
              throw new ReglaNegocioException("Ya existe un periodo académico con ese nombre");
            });
  }

  private void validarFechas(PeriodoAcademicoRequest solicitud) {
    if (solicitud.fechaFin().isBefore(solicitud.fechaInicio())) {
      throw new DatosInvalidosException(
          "La fecha de fin del periodo no puede ser anterior a la de inicio");
    }
    Set<TipoEtapa> tiposRecibidos = EnumSet.noneOf(TipoEtapa.class);
    for (EtapaPeriodoRequest etapa : solicitud.etapas()) {
      if (!tiposRecibidos.add(etapa.tipoEtapa())) {
        throw new DatosInvalidosException("Cada etapa solo puede aparecer una vez en el periodo");
      }
      validarFechasEtapa(etapa, solicitud);
    }
  }

  private void validarFechasEtapa(EtapaPeriodoRequest etapa, PeriodoAcademicoRequest periodo) {
    if (etapa.fechaCierre().isBefore(etapa.fechaApertura())) {
      throw new DatosInvalidosException(
          "El cierre de una etapa no puede ser anterior a su apertura");
    }
    boolean estaDentroDelPeriodo =
        !etapa.fechaApertura().isBefore(periodo.fechaInicio())
            && !etapa.fechaCierre().isAfter(periodo.fechaFin());
    if (!estaDentroDelPeriodo) {
      throw new DatosInvalidosException(
          "Las fechas de cada etapa deben estar dentro del periodo académico");
    }
  }

  private void aplicarEtapas(PeriodoAcademico periodo, List<EtapaPeriodoRequest> etapas) {
    Set<TipoEtapa> tiposVigentes = EnumSet.noneOf(TipoEtapa.class);
    for (EtapaPeriodoRequest etapa : etapas) {
      periodo.programarEtapa(etapa.tipoEtapa(), etapa.fechaApertura(), etapa.fechaCierre());
      tiposVigentes.add(etapa.tipoEtapa());
    }
    periodo.conservarSoloEtapas(tiposVigentes);
  }
}
