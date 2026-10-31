package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.DocenteResponse;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta de docentes respetando el alcance de quien pregunta. */
@Service
@Transactional(readOnly = true)
public class DocenteService {

  private final UsuarioRepository usuarioRepository;

  public DocenteService(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  /**
   * Lista los docentes activos que el usuario alcanza a ver. El filtro de un coordinador siempre es
   * su propia facultad, aunque pida otra.
   *
   * @param idFacultad facultad por la que filtra el vicerrector, o nulo para verlos todos
   * @param sesion usuario que consulta
   * @return docentes ordenados por nombre
   */
  public List<DocenteResponse> listarDocentes(Long idFacultad, UsuarioSesion sesion) {
    Long idFacultadConsulta = sesion.esCoordinador() ? sesion.idFacultad() : idFacultad;
    List<Usuario> docentes =
        idFacultadConsulta == null
            ? usuarioRepository.findByRolAndEsActivoTrueOrderByNombreCompletoAsc(Rol.DOCENTE)
            : usuarioRepository
                .findByRolAndProgramaFacultadIdFacultadAndEsActivoTrueOrderByNombreCompletoAsc(
                    Rol.DOCENTE, idFacultadConsulta);
    return docentes.stream().map(DocenteResponse::desde).toList();
  }

  /**
   * Obtiene un docente dentro del alcance del usuario.
   *
   * @param idDocente identificador del docente
   * @param sesion usuario que consulta
   * @return el docente solicitado
   * @throws RecursoNoEncontradoException si no existe o pertenece a otra facultad
   */
  public DocenteResponse obtenerDocente(Long idDocente, UsuarioSesion sesion) {
    return DocenteResponse.desde(buscarDocenteVisible(idDocente, sesion));
  }

  /**
   * Busca la entidad de un docente dentro del alcance del usuario. Un docente de otra facultad se
   * trata igual que uno inexistente para no revelar que existe.
   *
   * @param idDocente identificador del docente
   * @param sesion usuario que consulta
   * @return el docente
   * @throws RecursoNoEncontradoException si no existe o está fuera del alcance
   */
  public Usuario buscarDocenteVisible(Long idDocente, UsuarioSesion sesion) {
    return usuarioRepository
        .findByIdUsuarioAndRolAndEsActivoTrue(idDocente, Rol.DOCENTE)
        .filter(
            docente ->
                sesion.tieneAlcanceSobreFacultad(
                    docente.getPrograma().getFacultad().getIdFacultad()))
        .orElseThrow(() -> new RecursoNoEncontradoException("Docente no encontrado"));
  }
}
