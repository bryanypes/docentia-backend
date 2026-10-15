package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.FranjaRequest;
import com.uniautonoma.docentia.dto.FranjaResponse;
import com.uniautonoma.docentia.dto.LoteFranjasRequest;
import com.uniautonoma.docentia.dto.LoteFranjasRequest.AccionLote;
import com.uniautonoma.docentia.dto.ReporteLoteFranjasResponse;
import com.uniautonoma.docentia.dto.ReporteLoteFranjasResponse.Elemento;
import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.EstadoCita;
import com.uniautonoma.docentia.entity.EstadoFranja;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.DatosInvalidosException;
import com.uniautonoma.docentia.exception.EtapaCerradaException;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.CitaRepository;
import com.uniautonoma.docentia.repository.FranjaHorariaRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Franjas de 30 minutos que el coordinador abre para atender citas. Todo cambio ocurre en el
 * periodo activo y dentro de la etapa agenda de citas.
 */
@Service
@Transactional
public class FranjaService {

  private static final LocalTime ULTIMO_INICIO = LocalTime.of(23, 30);

  private final FranjaHorariaRepository franjaRepository;
  private final CitaRepository citaRepository;
  private final UsuarioRepository usuarioRepository;
  private final PeriodoAcademicoService periodoService;
  private final VentanaEtapaService ventanaEtapaService;

  public FranjaService(
      FranjaHorariaRepository franjaRepository,
      CitaRepository citaRepository,
      UsuarioRepository usuarioRepository,
      PeriodoAcademicoService periodoService,
      VentanaEtapaService ventanaEtapaService) {
    this.franjaRepository = franjaRepository;
    this.citaRepository = citaRepository;
    this.usuarioRepository = usuarioRepository;
    this.periodoService = periodoService;
    this.ventanaEtapaService = ventanaEtapaService;
  }

  /**
   * Abre una franja.
   *
   * @throws DatosInvalidosException si no dura 30 minutos, es del pasado o está fuera del periodo
   * @throws ReglaNegocioException si se cruza con otra franja abierta del coordinador
   * @throws EtapaCerradaException si la agenda de citas está cerrada
   */
  public FranjaResponse abrir(FranjaRequest solicitud, UsuarioSesion sesion) {
    PeriodoAcademico periodo = periodoEnAgenda();
    validarHorario(solicitud, periodo)
        .ifPresent(
            motivo -> {
              throw new DatosInvalidosException(motivo);
            });
    buscarSolape(sesion.idUsuario(), solicitud)
        .ifPresent(
            motivo -> {
              throw new ReglaNegocioException(motivo);
            });
    try {
      FranjaHoraria franja =
          franjaRepository.saveAndFlush(
              new FranjaHoraria(
                  usuarioRepository.getReferenceById(sesion.idUsuario()),
                  periodo,
                  solicitud.fecha(),
                  solicitud.horaInicio()));
      return FranjaResponse.desde(franja, null);
    } catch (DataIntegrityViolationException carrera) {
      // Otra petición abrió una franja que se cruza justo entre la revisión y el guardado.
      throw new ReglaNegocioException("La franja se cruza con otra franja abierta");
    }
  }

  /**
   * Abre o cierra varias franjas. Cada una se revisa por separado: las que no se pueden se omiten
   * con su motivo y no afectan a las demás. Nunca toca franjas con cita reservada.
   *
   * @throws EtapaCerradaException si la agenda de citas está cerrada
   */
  public ReporteLoteFranjasResponse aplicarLote(LoteFranjasRequest lote, UsuarioSesion sesion) {
    PeriodoAcademico periodo = periodoEnAgenda();
    List<Elemento> aplicadas = new ArrayList<>();
    List<Elemento> omitidas = new ArrayList<>();
    if (lote.accion() == AccionLote.ABRIR) {
      abrirLote(
          lote.franjas() == null ? List.of() : lote.franjas(),
          periodo,
          sesion,
          aplicadas,
          omitidas);
    } else {
      cerrarLote(lote.ids() == null ? List.of() : lote.ids(), sesion, aplicadas, omitidas);
    }
    return new ReporteLoteFranjasResponse(lote.accion(), aplicadas, omitidas);
  }

  /**
   * Cierra una franja propia; si ya estaba cerrada no cambia nada.
   *
   * @throws RecursoNoEncontradoException si no existe o es de otro coordinador
   * @throws ReglaNegocioException si tiene una cita reservada
   */
  public FranjaResponse cerrar(Long idFranja, UsuarioSesion sesion) {
    periodoEnAgenda();
    FranjaHoraria franja = buscarPropia(idFranja, sesion);
    if (citaRepository.existsByFranjaIdFranjaAndEstado(idFranja, EstadoCita.ACTIVA)) {
      throw new ReglaNegocioException("No se puede cerrar una franja con una cita reservada");
    }
    franja.cerrar();
    return FranjaResponse.desde(franja, null);
  }

  /**
   * Elimina una franja propia que nunca tuvo citas.
   *
   * @throws RecursoNoEncontradoException si no existe o es de otro coordinador
   * @throws ReglaNegocioException si tiene o tuvo citas
   */
  public void eliminar(Long idFranja, UsuarioSesion sesion) {
    periodoEnAgenda();
    FranjaHoraria franja = buscarPropia(idFranja, sesion);
    if (citaRepository.existsByFranjaIdFranjaAndEstado(idFranja, EstadoCita.ACTIVA)) {
      throw new ReglaNegocioException("No se puede eliminar una franja con una cita reservada");
    }
    if (citaRepository.existsByFranjaIdFranja(idFranja)) {
      throw new ReglaNegocioException(
          "La franja tiene historial de citas; ciérrala en lugar de eliminarla");
    }
    franjaRepository.delete(franja);
  }

  /**
   * Lista las franjas del coordinador en el periodo activo, con su reserva si la tienen.
   *
   * @param fecha filtro opcional por día
   * @param estado filtro opcional por estado
   */
  @Transactional(readOnly = true)
  public List<FranjaResponse> listar(LocalDate fecha, EstadoFranja estado, UsuarioSesion sesion) {
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    List<FranjaHoraria> franjas =
        franjaRepository.buscarDelCoordinador(
            sesion.idUsuario(), periodo.getIdPeriodo(), fecha, estado);
    Map<Long, Cita> reservas =
        franjas.isEmpty()
            ? Map.of()
            : citaRepository
                .buscarPorFranjas(
                    franjas.stream().map(FranjaHoraria::getIdFranja).toList(), EstadoCita.ACTIVA)
                .stream()
                .collect(
                    Collectors.toMap(cita -> cita.getFranja().getIdFranja(), Function.identity()));
    return franjas.stream()
        .map(franja -> FranjaResponse.desde(franja, reservas.get(franja.getIdFranja())))
        .toList();
  }

  private void abrirLote(
      List<FranjaRequest> franjas,
      PeriodoAcademico periodo,
      UsuarioSesion sesion,
      List<Elemento> aplicadas,
      List<Elemento> omitidas) {
    Usuario coordinador = usuarioRepository.getReferenceById(sesion.idUsuario());
    List<FranjaRequest> aceptadas = new ArrayList<>();
    for (int indice = 0; indice < franjas.size(); indice++) {
      FranjaRequest solicitud = franjas.get(indice);
      Optional<String> motivo =
          validarHorario(solicitud, periodo)
              .or(() -> buscarSolapeEnLote(solicitud, aceptadas))
              .or(() -> buscarSolape(sesion.idUsuario(), solicitud));
      if (motivo.isPresent()) {
        omitidas.add(elemento(indice, null, solicitud, motivo.get()));
        continue;
      }
      // Con identidad generada por la base, save inserta de inmediato y ya trae el identificador.
      FranjaHoraria franja =
          franjaRepository.save(
              new FranjaHoraria(coordinador, periodo, solicitud.fecha(), solicitud.horaInicio()));
      aceptadas.add(solicitud);
      aplicadas.add(elemento(indice, franja, solicitud, null));
    }
  }

  private void cerrarLote(
      List<Long> ids, UsuarioSesion sesion, List<Elemento> aplicadas, List<Elemento> omitidas) {
    for (int indice = 0; indice < ids.size(); indice++) {
      Long idFranja = ids.get(indice);
      Optional<FranjaHoraria> franja =
          idFranja == null
              ? Optional.empty()
              : franjaRepository.findByIdFranjaAndCoordinadorIdUsuario(
                  idFranja, sesion.idUsuario());
      if (franja.isEmpty()) {
        omitidas.add(new Elemento(indice, idFranja, null, null, null, "Franja no encontrada"));
      } else if (!franja.get().estaAbierta()) {
        omitidas.add(elemento(indice, franja.get(), null, "La franja ya estaba cerrada"));
      } else if (citaRepository.existsByFranjaIdFranjaAndEstado(idFranja, EstadoCita.ACTIVA)) {
        omitidas.add(elemento(indice, franja.get(), null, "Tiene una cita reservada"));
      } else {
        franja.get().cerrar();
        aplicadas.add(elemento(indice, franja.get(), null, null));
      }
    }
  }

  /** Reglas de la franja que no dependen de las demás. */
  private Optional<String> validarHorario(FranjaRequest solicitud, PeriodoAcademico periodo) {
    if (solicitud.fecha() == null
        || solicitud.horaInicio() == null
        || solicitud.horaFin() == null) {
      return Optional.of("Faltan la fecha o las horas de la franja");
    }
    if (solicitud.horaInicio().getSecond() != 0 || solicitud.horaInicio().getNano() != 0) {
      return Optional.of("La hora de inicio debe ser en minutos exactos");
    }
    if (!solicitud
            .horaFin()
            .equals(solicitud.horaInicio().plusMinutes(FranjaHoraria.DURACION_MINUTOS))
        || solicitud.horaInicio().isAfter(ULTIMO_INICIO)) {
      return Optional.of("La franja debe durar exactamente 30 minutos dentro del mismo día");
    }
    if (solicitud.fecha().isBefore(ventanaEtapaService.hoy())) {
      return Optional.of("No se pueden abrir franjas en fechas pasadas");
    }
    if (solicitud.fecha().isBefore(periodo.getFechaInicio())
        || solicitud.fecha().isAfter(periodo.getFechaFin())) {
      return Optional.of("La fecha debe estar dentro del periodo " + periodo.getNombre());
    }
    return Optional.empty();
  }

  private Optional<String> buscarSolape(Long idCoordinador, FranjaRequest solicitud) {
    return franjaRepository
        .buscarSolapada(
            idCoordinador, solicitud.fecha(), solicitud.horaInicio(), solicitud.horaFin())
        .map(
            existente ->
                "Se cruza con la franja abierta del "
                    + existente.getFecha()
                    + " de "
                    + existente.getHoraInicio()
                    + " a "
                    + existente.getHoraFin());
  }

  private Optional<String> buscarSolapeEnLote(
      FranjaRequest solicitud, List<FranjaRequest> aceptadas) {
    return aceptadas.stream()
        .filter(
            otra ->
                otra.fecha().equals(solicitud.fecha())
                    && otra.horaInicio().isBefore(solicitud.horaFin())
                    && solicitud.horaInicio().isBefore(otra.horaFin()))
        .findFirst()
        .map(otra -> "Se cruza con otra franja del mismo lote (" + otra.horaInicio() + ")");
  }

  private PeriodoAcademico periodoEnAgenda() {
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    ventanaEtapaService.exigirEtapaAbierta(periodo, TipoEtapa.AGENDA_CITAS);
    return periodo;
  }

  private FranjaHoraria buscarPropia(Long idFranja, UsuarioSesion sesion) {
    return franjaRepository
        .findByIdFranjaAndCoordinadorIdUsuario(idFranja, sesion.idUsuario())
        .orElseThrow(() -> new RecursoNoEncontradoException("Franja no encontrada"));
  }

  private Elemento elemento(
      int indice, FranjaHoraria franja, FranjaRequest solicitud, String motivo) {
    if (franja != null) {
      return new Elemento(
          indice,
          franja.getIdFranja(),
          franja.getFecha(),
          franja.getHoraInicio(),
          franja.getHoraFin(),
          motivo);
    }
    return new Elemento(
        indice, null, solicitud.fecha(), solicitud.horaInicio(), solicitud.horaFin(), motivo);
  }
}
