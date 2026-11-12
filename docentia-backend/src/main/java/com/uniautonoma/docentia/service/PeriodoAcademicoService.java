package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.CalendarioResponse;
import com.uniautonoma.docentia.dto.EtapaPeriodoRequest;
import com.uniautonoma.docentia.dto.PeriodoAcademicoRequest;
import com.uniautonoma.docentia.dto.PeriodoAcademicoResponse;
import com.uniautonoma.docentia.dto.PeriodoResumenResponse;
import com.uniautonoma.docentia.dto.VentanaEtapaResponse;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.exception.DatosInvalidosException;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas de negocio de los periodos académicos y sus etapas. */
@Service
@Transactional
public class PeriodoAcademicoService {

  private final PeriodoAcademicoRepository periodoRepository;
  private final VentanaEtapaService ventanaEtapaService;

  /** Formato del nombre de un periodo: año y semestre, por ejemplo 2027-1. */
  private static final Pattern FORMATO_NOMBRE = Pattern.compile("[0-9]{4}-[12]");

  /** Último mes del primer semestre; por confirmar con la institución. */
  @Value("${docentia.periodos.mes-fin-primer-semestre:6}")
  private int mesFinPrimerSemestre = 6;

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

  /** Etapa vigente y línea de tiempo del periodo activo. */
  @Transactional(readOnly = true)
  public CalendarioResponse describirCalendario() {
    PeriodoAcademico periodo = exigirPeriodoActivo();
    List<VentanaEtapaResponse> etapas = ventanaEtapaService.describirVentanas(periodo);
    VentanaEtapaResponse vigente =
        etapas.stream().filter(VentanaEtapaResponse::abierta).reduce((a, b) -> b).orElse(null);
    return new CalendarioResponse(
        PeriodoResumenResponse.desde(periodo),
        ventanaEtapaService.hoy(),
        vigente,
        etapas,
        "Seguimiento del plan: período siguiente");
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
    return crearPeriodo(solicitud, null);
  }

  /**
   * Registra un periodo con el nombre calculado desde su fecha de inicio (AAAA-1 o AAAA-2).
   *
   * @param solicitud datos del periodo; el nombre puede venir vacío
   * @param puedeFijarNombre si quien llama puede ajustar el nombre calculado (administración); null
   *     en llamadas internas, que respetan el nombre recibido
   * @return el periodo creado
   * @throws DatosInvalidosException si el nombre no tiene el formato o no se permite ajustarlo
   */
  public PeriodoAcademicoResponse crearPeriodo(
      PeriodoAcademicoRequest solicitud, Boolean puedeFijarNombre) {
    validarFechas(solicitud);
    String nombre = resolverNombre(solicitud, null, puedeFijarNombre);
    validarNombreDisponible(nombre, null, true);
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
    return actualizarPeriodo(idPeriodo, solicitud, null);
  }

  /**
   * Actualiza un periodo; conservar su nombre siempre se permite, cambiarlo sigue las reglas de
   * creación.
   *
   * @param idPeriodo identificador del periodo
   * @param solicitud datos nuevos
   * @param puedeFijarNombre si quien llama puede ajustar el nombre calculado (administración); null
   *     en llamadas internas, que respetan el nombre recibido
   * @return el periodo actualizado
   */
  public PeriodoAcademicoResponse actualizarPeriodo(
      Long idPeriodo, PeriodoAcademicoRequest solicitud, Boolean puedeFijarNombre) {
    PeriodoAcademico periodo = buscarPeriodo(idPeriodo);
    validarFechas(solicitud);
    String nombre = resolverNombre(solicitud, periodo.getNombre(), puedeFijarNombre);
    validarNombreDisponible(nombre, idPeriodo, !nombre.equals(periodo.getNombre()));
    periodo.setNombre(nombre);
    periodo.setFechaInicio(solicitud.fechaInicio());
    periodo.setFechaFin(solicitud.fechaFin());
    aplicarEtapas(periodo, solicitud.etapas());
    return PeriodoAcademicoResponse.desde(periodo);
  }

  /**
   * Activa un periodo y desactiva el que estuviera activo, de modo que solo quede uno.
   *
   * <p>Las clases y matrículas cuelgan de un periodo: si el que se activa no tiene matrículas, los
   * estudiantes dejan de ver a sus profesores. Por eso se exige confirmación explícita; no se
   * bloquea del todo porque la matrícula de un periodo nuevo solo se importa con él ya activo.
   *
   * @param idPeriodo identificador del periodo que pasa a ser el activo
   * @param confirmado si quien llama acepta activarlo aunque no tenga matrículas
   * @return el periodo ya activo
   * @throws RecursoNoEncontradoException si el periodo no existe
   * @throws ReglaNegocioException si no tiene matrículas y no se confirmó
   */
  public PeriodoAcademicoResponse activarPeriodo(Long idPeriodo, boolean confirmado) {
    PeriodoAcademico solicitado = buscarPeriodo(idPeriodo);
    if (solicitado.getEsActivo()) {
      return PeriodoAcademicoResponse.desde(solicitado);
    }
    if (!confirmado && periodoRepository.contarMatriculasActivas(idPeriodo) == 0) {
      throw new ReglaNegocioException(
          "El periodo "
              + solicitado.getNombre()
              + " no tiene matrículas: al activarlo, los estudiantes dejarán de ver a sus"
              + " profesores hasta que se importe la matrícula. Confirma para activarlo igual");
    }
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

  private void validarNombreDisponible(
      String nombre, Long idPeriodoActual, boolean comprobarParecido) {
    periodoRepository
        .findByNombre(nombre)
        .filter(existente -> !existente.getIdPeriodo().equals(idPeriodoActual))
        .ifPresent(
            existente -> {
              throw new ReglaNegocioException("Ya existe un periodo académico con ese nombre");
            });
    if (!comprobarParecido) {
      return;
    }
    String clave = clavePeriodo(nombre);
    periodoRepository.findAll().stream()
        .filter(existente -> !existente.getIdPeriodo().equals(idPeriodoActual))
        .filter(existente -> clavePeriodo(existente.getNombre()).equals(clave))
        .findFirst()
        .ifPresent(
            existente -> {
              throw new ReglaNegocioException(
                  "Ya existe el periodo "
                      + existente.getNombre()
                      + ", que es el mismo semestre con otro formato de nombre. Usa ese periodo");
            });
  }

  /**
   * Nombre que corresponde a una fecha de inicio: AAAA-1 hasta el mes de corte, AAAA-2 después.
   *
   * @param fechaInicio inicio del periodo
   * @return nombre calculado
   */
  public String nombreCalculado(java.time.LocalDate fechaInicio) {
    return fechaInicio.getYear()
        + (fechaInicio.getMonthValue() <= mesFinPrimerSemestre ? "-1" : "-2");
  }

  /**
   * Decide el nombre que se guarda. Vacío o igual al de hoy: se usa sin más. Un nombre distinto al
   * calculado solo lo fija administración, y siempre con formato AAAA-1 o AAAA-2.
   */
  private String resolverNombre(
      PeriodoAcademicoRequest solicitud, String nombreActual, Boolean puedeFijarNombre) {
    String pedido = solicitud.nombre() == null ? "" : solicitud.nombre().trim();
    String calculado = nombreCalculado(solicitud.fechaInicio());
    // Llamadas internas (importación, pruebas): el nombre recibido se respeta tal cual.
    if (puedeFijarNombre == null && !pedido.isEmpty()) {
      return pedido;
    }
    if (pedido.isEmpty()) {
      return nombreActual != null ? nombreActual : calculado;
    }
    if (pedido.equals(nombreActual) || pedido.equals(calculado)) {
      return pedido;
    }
    if (!Boolean.TRUE.equals(puedeFijarNombre)) {
      throw new DatosInvalidosException(
          "El nombre se calcula a partir de la fecha de inicio ("
              + calculado
              + "); solo administración puede ajustarlo",
          "nombre");
    }
    if (!FORMATO_NOMBRE.matcher(pedido).matches()) {
      throw new DatosInvalidosException(
          "El nombre debe tener el formato AAAA-1 o AAAA-2, por ejemplo " + calculado, "nombre");
    }
    return pedido;
  }

  /** Iguala nombres que solo difieren en el cero del semestre: 2026-02 y 2026-2. */
  private static String clavePeriodo(String nombre) {
    return nombre.trim().toLowerCase(Locale.ROOT).replaceAll("-0+(\\d)", "-$1");
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
