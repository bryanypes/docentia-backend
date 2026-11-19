package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.PerfilResponse;
import com.uniautonoma.docentia.dto.TutorialResponse;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Perfil propio: solo lectura, siempre del usuario de la sesión. */
@Service
public class PerfilService {

  private final UsuarioRepository usuarioRepository;

  public PerfilService(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  /**
   * @throws RecursoNoEncontradoException si el usuario de la sesión ya no existe
   */
  @Transactional(readOnly = true)
  public PerfilResponse obtener(UsuarioSesion sesion) {
    Usuario usuario = propio(sesion);
    Programa programa = usuario.getPrograma();
    Facultad facultad = programa != null ? programa.getFacultad() : usuario.getFacultad();
    return new PerfilResponse(
        usuario.getIdUsuario(),
        usuario.getNombreCompleto(),
        usuario.getCorreo(),
        usuario.getCodigo(),
        usuario.getRol(),
        facultad != null ? facultad.getNombre() : null,
        programa != null ? programa.getNombre() : null,
        usuario.getFechaCreacion(),
        usuario.getDebeCambiarContrasena());
  }

  /** Si ya vio el tutorial guiado del primer inicio de sesión. */
  @Transactional(readOnly = true)
  public TutorialResponse estadoTutorial(UsuarioSesion sesion) {
    return new TutorialResponse(propio(sesion).getTutorialVisto());
  }

  /** Lo terminó o lo saltó: no se le vuelve a mostrar al entrar. */
  @Transactional
  public TutorialResponse marcarTutorialVisto(UsuarioSesion sesion) {
    Usuario usuario = propio(sesion);
    usuario.marcarTutorialVisto();
    return new TutorialResponse(true);
  }

  /** Quiere verlo otra vez: se le muestra en la próxima carga. */
  @Transactional
  public TutorialResponse reiniciarTutorial(UsuarioSesion sesion) {
    Usuario usuario = propio(sesion);
    usuario.reiniciarTutorial();
    return new TutorialResponse(false);
  }

  private Usuario propio(UsuarioSesion sesion) {
    return usuarioRepository
        .findById(sesion.idUsuario())
        .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
  }
}
