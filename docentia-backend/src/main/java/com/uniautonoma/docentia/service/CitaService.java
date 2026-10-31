package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.CitaResponse;
import com.uniautonoma.docentia.dto.FranjaDisponibleResponse;
import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.EstadoCita;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import com.uniautonoma.docentia.entity.MotivoLiberacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.exception.EtapaCerradaException;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.CitaRepository;
import com.uniautonoma.docentia.repository.FranjaHorariaRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reserva de franjas por los docentes. Un docente tiene a lo sumo una cita activa por periodo:
 * reservar otra libera la anterior en la misma transacción, así que si algo falla conserva la que
 * tenía.
 */
@Service
@Transactional
public class CitaService {

  private static final String FRANJA_NO_ENCONTRADA = "Franja no encontrada";

  private final FranjaHorariaRepository franjaRepository;
  private final CitaRepository citaRepository;
  private final UsuarioRepository usuarioRepository;
  private final PeriodoAcademicoService periodoService;
  private final VentanaEtapaService ventanaEtapaService;

  public CitaService(
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
   * Franjas que el docente puede reservar: abiertas, sin reserva, de hoy en adelante y de un
   * coordinador de su facultad, en el periodo activo.
   */
  @Transactional(readOnly = true)
  public List<FranjaDisponibleResponse> listarDisponibles(UsuarioSesion sesion) {
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    return franjaRepository
        .buscarDisponibles(periodo.getIdPeriodo(), sesion.idFacultad(), ventanaEtapaService.hoy())
        .stream()
        .map(FranjaDisponibleResponse::desde)
        .toList();
  }

  /**
   * Reserva una franja para el docente. Si ya tenía una cita activa en el periodo, la libera en la
   * misma transacción.
   *
   * @return la cita nueva, con el identificador de la que se liberó
   * @throws RecursoNoEncontradoException si la franja no existe, es de otro periodo o de un
   *     coordinador de otra facultad
   * @throws ReglaNegocioException si la franja está cerrada, ya pasó o ya está reservada
   * @throws EtapaCerradaException si la agenda de citas está cerrada
   */
  public CitaResponse reservar(Long idFranja, UsuarioSesion sesion) {
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    ventanaEtapaService.exigirEtapaAbierta(periodo, TipoEtapa.AGENDA_CITAS);

    // 1. Bloquear la franja: otro docente que la quiera espera aquí hasta que esta transacción
    // termine, y entonces ve la reserva.
    FranjaHoraria franja =
        franjaRepository
            .bloquear(idFranja)
            .filter(
                encontrada -> encontrada.getPeriodo().getIdPeriodo().equals(periodo.getIdPeriodo()))
            .filter(
                encontrada ->
                    Objects.equals(
                        encontrada.getCoordinador().getFacultad().getIdFacultad(),
                        sesion.idFacultad()))
            .orElseThrow(() -> new RecursoNoEncontradoException(FRANJA_NO_ENCONTRADA));
    if (!franja.estaAbierta()) {
      throw new ReglaNegocioException("La franja está cerrada");
    }
    if (franja.getFecha().isBefore(ventanaEtapaService.hoy())) {
      throw new ReglaNegocioException("La franja ya pasó");
    }
    Optional<Cita> ocupada =
        citaRepository.findByFranjaIdFranjaAndEstado(idFranja, EstadoCita.ACTIVA);
    if (ocupada.isPresent()) {
      throw new ReglaNegocioException(
          ocupada.get().getDocente().getIdUsuario().equals(sesion.idUsuario())
              ? "Ya tienes reservada esta franja"
              : "La franja ya fue reservada por otro docente");
    }

    // 2. Liberar la cita anterior del docente en el periodo, si tiene.
    Long idLiberada = null;
    Optional<Cita> anterior =
        citaRepository.bloquearActiva(sesion.idUsuario(), periodo.getIdPeriodo());
    if (anterior.isPresent()) {
      anterior.get().liberar(MotivoLiberacion.REEMPLAZADA);
      citaRepository.saveAndFlush(anterior.get());
      idLiberada = anterior.get().getIdCita();
    }

    // 3. Crear la nueva. Los índices únicos de la base respaldan las dos reglas.
    try {
      Cita nueva =
          citaRepository.saveAndFlush(
              new Cita(franja, usuarioRepository.getReferenceById(sesion.idUsuario())));
      return CitaResponse.desde(nueva, idLiberada);
    } catch (DataIntegrityViolationException choque) {
      throw new ReglaNegocioException(
          "No se pudo completar la reserva porque cambió al mismo tiempo; vuelve a intentarlo");
    }
  }

  /**
   * Cita activa del docente en el periodo activo.
   *
   * @return la cita, o vacío si no tiene
   */
  @Transactional(readOnly = true)
  public Optional<CitaResponse> buscarMiCita(UsuarioSesion sesion) {
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    return citaRepository
        .buscarActiva(sesion.idUsuario(), periodo.getIdPeriodo())
        .map(cita -> CitaResponse.desde(cita, null));
  }
}
