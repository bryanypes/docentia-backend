package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.UsuarioRequest;
import com.uniautonoma.docentia.dto.UsuarioResponse;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administración de usuarios: rol y vínculo a facultad o programa. La hace la vicerrectoría porque
 * todavía no existe un rol de administrador.
 */
@RestController
@PreAuthorize("hasRole('VICERRECTOR')")
@RequestMapping("/usuarios")
public class UsuarioController {

  private final UsuarioService usuarioService;

  public UsuarioController(UsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  /**
   * Lista los usuarios.
   *
   * @param rol filtro opcional por rol
   * @return usuarios con su alcance
   */
  @GetMapping
  public List<UsuarioResponse> listarUsuarios(@RequestParam(required = false) Rol rol) {
    return usuarioService.listarUsuarios(rol);
  }

  /**
   * Consulta un usuario.
   *
   * @param idUsuario identificador del usuario
   * @return el usuario con su alcance
   */
  @GetMapping("/{idUsuario}")
  public UsuarioResponse obtenerUsuario(@PathVariable Long idUsuario) {
    return usuarioService.obtenerUsuario(idUsuario);
  }

  /**
   * Crea un usuario.
   *
   * @param solicitud datos, rol y vínculo
   * @return el usuario creado
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UsuarioResponse crearUsuario(@Valid @RequestBody UsuarioRequest solicitud) {
    return usuarioService.crearUsuario(solicitud);
  }

  /**
   * Edita un usuario.
   *
   * @param idUsuario identificador del usuario
   * @param solicitud datos, rol y vínculo nuevos
   * @return el usuario actualizado
   */
  @PutMapping("/{idUsuario}")
  public UsuarioResponse actualizarUsuario(
      @PathVariable Long idUsuario, @Valid @RequestBody UsuarioRequest solicitud) {
    return usuarioService.actualizarUsuario(idUsuario, solicitud);
  }
}
