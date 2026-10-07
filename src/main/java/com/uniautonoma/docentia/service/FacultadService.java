package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.FacultadRequest;
import com.uniautonoma.docentia.dto.FacultadResponse;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.FacultadRepository;
import com.uniautonoma.docentia.repository.ProgramaRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas de negocio de las facultades. */
@Service
@Transactional
public class FacultadService {

  private static final String FACULTAD_NO_ENCONTRADA = "Facultad no encontrada";

  private final FacultadRepository facultadRepository;
  private final ProgramaRepository programaRepository;

  public FacultadService(
      FacultadRepository facultadRepository, ProgramaRepository programaRepository) {
    this.facultadRepository = facultadRepository;
    this.programaRepository = programaRepository;
  }

  /**
   * Lista las facultades vigentes que el usuario alcanza a ver.
   *
   * @param sesion usuario que consulta
   * @return facultades activas ordenadas por nombre; para un coordinador, solo la suya
   */
  @Transactional(readOnly = true)
  public List<FacultadResponse> listarFacultades(UsuarioSesion sesion) {
    return facultadRepository.findByEsActivaTrueOrderByNombreAsc().stream()
        .filter(facultad -> sesion.tieneAlcanceSobreFacultad(facultad.getIdFacultad()))
        .map(FacultadResponse::desde)
        .toList();
  }

  /**
   * Obtiene una facultad vigente dentro del alcance del usuario.
   *
   * @param idFacultad identificador de la facultad
   * @param sesion usuario que consulta
   * @return la facultad solicitada
   * @throws RecursoNoEncontradoException si no existe, fue dada de baja o está fuera del alcance
   */
  @Transactional(readOnly = true)
  public FacultadResponse obtenerFacultad(Long idFacultad, UsuarioSesion sesion) {
    if (!sesion.tieneAlcanceSobreFacultad(idFacultad)) {
      throw new RecursoNoEncontradoException(FACULTAD_NO_ENCONTRADA);
    }
    return FacultadResponse.desde(buscarFacultadActiva(idFacultad));
  }

  /**
   * Registra una facultad nueva.
   *
   * @param solicitud datos de la facultad
   * @return la facultad creada
   * @throws ReglaNegocioException si ya hay una facultad vigente con ese nombre
   */
  public FacultadResponse crearFacultad(FacultadRequest solicitud) {
    String nombre = solicitud.nombre().trim();
    validarNombreDisponible(nombre, null);
    return FacultadResponse.desde(facultadRepository.save(new Facultad(nombre)));
  }

  /**
   * Cambia el nombre de una facultad.
   *
   * @param idFacultad identificador de la facultad
   * @param solicitud datos nuevos
   * @return la facultad actualizada
   * @throws RecursoNoEncontradoException si la facultad no existe
   * @throws ReglaNegocioException si el nombre ya lo usa otra facultad
   */
  public FacultadResponse actualizarFacultad(Long idFacultad, FacultadRequest solicitud) {
    Facultad facultad = buscarFacultadActiva(idFacultad);
    String nombre = solicitud.nombre().trim();
    validarNombreDisponible(nombre, idFacultad);
    facultad.setNombre(nombre);
    return FacultadResponse.desde(facultad);
  }

  /**
   * Da de baja una facultad. El registro se conserva porque el histórico de evaluaciones lo
   * referencia.
   *
   * @param idFacultad identificador de la facultad
   * @throws RecursoNoEncontradoException si la facultad no existe
   * @throws ReglaNegocioException si la facultad todavía tiene programas activos
   */
  public void eliminarFacultad(Long idFacultad) {
    Facultad facultad = buscarFacultadActiva(idFacultad);
    if (programaRepository.existsByFacultadIdFacultadAndEsActivoTrue(idFacultad)) {
      throw new ReglaNegocioException(
          "No se puede eliminar la facultad porque tiene programas activos");
    }
    facultad.setEsActiva(false);
  }

  /**
   * Busca la entidad de una facultad vigente.
   *
   * @param idFacultad identificador de la facultad
   * @return la facultad
   * @throws RecursoNoEncontradoException si no existe o fue dada de baja
   */
  @Transactional(readOnly = true)
  public Facultad buscarFacultadActiva(Long idFacultad) {
    return facultadRepository
        .findByIdFacultadAndEsActivaTrue(idFacultad)
        .orElseThrow(() -> new RecursoNoEncontradoException(FACULTAD_NO_ENCONTRADA));
  }

  private void validarNombreDisponible(String nombre, Long idFacultadActual) {
    facultadRepository
        .findByNombreIgnoreCaseAndEsActivaTrue(nombre)
        .filter(existente -> !existente.getIdFacultad().equals(idFacultadActual))
        .ifPresent(
            existente -> {
              throw new ReglaNegocioException("Ya existe una facultad con ese nombre");
            });
  }
}
