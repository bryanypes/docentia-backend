package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.ProgramaRequest;
import com.uniautonoma.docentia.dto.ProgramaResponse;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.ProgramaRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas de negocio de los programas académicos. */
@Service
@Transactional
public class ProgramaService {

  private static final String PROGRAMA_NO_ENCONTRADO = "Programa no encontrado";

  private final ProgramaRepository programaRepository;
  private final FacultadService facultadService;

  public ProgramaService(ProgramaRepository programaRepository, FacultadService facultadService) {
    this.programaRepository = programaRepository;
    this.facultadService = facultadService;
  }

  /**
   * Lista los programas vigentes que el usuario alcanza a ver. Para un coordinador siempre son los
   * de su facultad, sin importar el filtro que envíe.
   *
   * @param idFacultad facultad por la que se filtra, o nulo para traerlos todos
   * @param sesion usuario que consulta
   * @return programas activos ordenados por nombre
   */
  @Transactional(readOnly = true)
  public List<ProgramaResponse> listarProgramas(Long idFacultad, UsuarioSesion sesion) {
    Long idFacultadConsulta = sesion.esCoordinador() ? sesion.idFacultad() : idFacultad;
    List<Programa> programas =
        idFacultadConsulta == null
            ? programaRepository.findByEsActivoTrueOrderByNombreAsc()
            : programaRepository.findByFacultadIdFacultadAndEsActivoTrueOrderByNombreAsc(
                idFacultadConsulta);
    return programas.stream().map(ProgramaResponse::desde).toList();
  }

  /**
   * Obtiene un programa vigente dentro del alcance del usuario.
   *
   * @param idPrograma identificador del programa
   * @param sesion usuario que consulta
   * @return el programa solicitado
   * @throws RecursoNoEncontradoException si no existe, fue dado de baja o es de otra facultad
   */
  @Transactional(readOnly = true)
  public ProgramaResponse obtenerPrograma(Long idPrograma, UsuarioSesion sesion) {
    Programa programa = buscarProgramaActivo(idPrograma);
    if (!sesion.tieneAlcanceSobreFacultad(programa.getFacultad().getIdFacultad())) {
      throw new RecursoNoEncontradoException(PROGRAMA_NO_ENCONTRADO);
    }
    return ProgramaResponse.desde(programa);
  }

  /**
   * Registra un programa dentro de una facultad.
   *
   * @param solicitud nombre del programa y facultad a la que pertenece
   * @return el programa creado
   * @throws RecursoNoEncontradoException si la facultad no existe
   * @throws ReglaNegocioException si la facultad ya tiene un programa con ese nombre
   */
  public ProgramaResponse crearPrograma(ProgramaRequest solicitud) {
    Facultad facultad = facultadService.buscarFacultadActiva(solicitud.idFacultad());
    String nombre = solicitud.nombre().trim();
    validarNombreDisponible(facultad.getIdFacultad(), nombre, null);
    return ProgramaResponse.desde(programaRepository.save(new Programa(nombre, facultad)));
  }

  /**
   * Actualiza el nombre de un programa o lo traslada a otra facultad.
   *
   * @param idPrograma identificador del programa
   * @param solicitud datos nuevos
   * @return el programa actualizado
   * @throws RecursoNoEncontradoException si el programa o la facultad no existen
   * @throws ReglaNegocioException si el nombre ya está en uso dentro de la facultad
   */
  public ProgramaResponse actualizarPrograma(Long idPrograma, ProgramaRequest solicitud) {
    Programa programa = buscarProgramaActivo(idPrograma);
    Facultad facultad = facultadService.buscarFacultadActiva(solicitud.idFacultad());
    String nombre = solicitud.nombre().trim();
    validarNombreDisponible(facultad.getIdFacultad(), nombre, idPrograma);
    programa.setNombre(nombre);
    programa.setFacultad(facultad);
    return ProgramaResponse.desde(programa);
  }

  /**
   * Da de baja un programa conservando su registro.
   *
   * @param idPrograma identificador del programa
   * @throws RecursoNoEncontradoException si el programa no existe
   */
  public void eliminarPrograma(Long idPrograma) {
    buscarProgramaActivo(idPrograma).setEsActivo(false);
  }

  private Programa buscarProgramaActivo(Long idPrograma) {
    return programaRepository
        .findByIdProgramaAndEsActivoTrue(idPrograma)
        .orElseThrow(() -> new RecursoNoEncontradoException(PROGRAMA_NO_ENCONTRADO));
  }

  private void validarNombreDisponible(Long idFacultad, String nombre, Long idProgramaActual) {
    programaRepository
        .findByFacultadIdFacultadAndNombreIgnoreCaseAndEsActivoTrue(idFacultad, nombre)
        .filter(existente -> !existente.getIdPrograma().equals(idProgramaActual))
        .ifPresent(
            existente -> {
              throw new ReglaNegocioException("La facultad ya tiene un programa con ese nombre");
            });
  }
}
