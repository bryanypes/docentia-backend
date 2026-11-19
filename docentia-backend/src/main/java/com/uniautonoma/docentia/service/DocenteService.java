package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.DocenteResponse;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta de docentes respetando el alcance de quien pregunta. Los coordinadores también son
 * docentes evaluados, pero solo la vicerrectoría los ve como tales: un coordinador nunca ve a otro
 * coordinador ni a sí mismo en la lista de su facultad.
 */
@Service
@Transactional(readOnly = true)
public class DocenteService {

  private final UsuarioRepository usuarioRepository;

  public DocenteService(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  /**
   * Lista los docentes activos que el usuario alcanza a ver. El filtro de un coordinador siempre es
   * su propia facultad, aunque pida otra. La vicerrectoría ve además a los coordinadores.
   *
   * @param idFacultad facultad por la que filtra el vicerrector, o nulo para verlos todos
   * @param sesion usuario que consulta
   * @return docentes ordenados por nombre
   */
  public List<DocenteResponse> listarDocentes(Long idFacultad, UsuarioSesion sesion) {
    Long idFacultadConsulta = sesion.esCoordinador() ? sesion.idFacultad() : idFacultad;
    List<Usuario> docentes = new ArrayList<>(buscarActivos(Rol.DOCENTE, idFacultadConsulta));
    if (sesion.esVicerrector()) {
      docentes.addAll(buscarActivos(Rol.COORDINADOR, idFacultadConsulta));
      docentes.sort(Comparator.comparing(Usuario::getNombreCompleto));
    }
    return docentes.stream().map(DocenteResponse::desde).toList();
  }

  /**
   * Lista solo los docentes que el usuario evalúa: la coordinación, los docentes de su facultad; la
   * vicerrectoría, los coordinadores.
   *
   * @param sesion evaluador
   * @return evaluados ordenados por nombre
   */
  public List<DocenteResponse> listarEvaluados(UsuarioSesion sesion) {
    List<Usuario> evaluados =
        sesion.esVicerrector()
            ? buscarActivos(Rol.COORDINADOR, null)
            : buscarActivos(Rol.DOCENTE, sesion.idFacultad());
    return evaluados.stream().map(DocenteResponse::desde).toList();
  }

  private List<Usuario> buscarActivos(Rol rol, Long idFacultad) {
    if (idFacultad == null) {
      return usuarioRepository.findByRolAndEsActivoTrueOrderByNombreCompletoAsc(rol);
    }
    return rol == Rol.COORDINADOR
        ? usuarioRepository.findByRolAndFacultadIdFacultadAndEsActivoTrueOrderByNombreCompletoAsc(
            rol, idFacultad)
        : usuarioRepository
            .findByRolAndProgramaFacultadIdFacultadAndEsActivoTrueOrderByNombreCompletoAsc(
                rol, idFacultad);
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
   * trata igual que uno inexistente para no revelar que existe. Un coordinador evaluado solo es
   * visible para la vicerrectoría.
   *
   * @param idDocente identificador del docente
   * @param sesion usuario que consulta
   * @return el docente
   * @throws RecursoNoEncontradoException si no existe o está fuera del alcance
   */
  public Usuario buscarDocenteVisible(Long idDocente, UsuarioSesion sesion) {
    return usuarioRepository
        .findByIdUsuarioAndRolInAndEsActivoTrue(idDocente, Rol.EVALUABLES)
        .filter(docente -> docente.getRol() == Rol.DOCENTE || sesion.esVicerrector())
        .filter(
            docente ->
                sesion.tieneAlcanceSobreFacultad(
                    docente.getFacultadDeAdscripcion().getIdFacultad()))
        .orElseThrow(() -> new RecursoNoEncontradoException("Docente no encontrado"));
  }

  /**
   * Busca a un docente que el usuario evalúa directamente (coevaluación, cita y acta): un docente
   * de su facultad si es coordinador, o un coordinador si es vicerrector.
   *
   * @param idDocente identificador del evaluado
   * @param sesion evaluador
   * @return el evaluado
   * @throws RecursoNoEncontradoException si no existe o no le corresponde evaluarlo
   */
  public Usuario buscarEvaluadoDe(Long idDocente, UsuarioSesion sesion) {
    Usuario docente = buscarDocenteVisible(idDocente, sesion);
    if (docente.getRol().evaluador() != sesion.rol()) {
      throw new RecursoNoEncontradoException("Docente no encontrado");
    }
    return docente;
  }
}
