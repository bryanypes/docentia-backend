package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.UsuarioRequest;
import com.uniautonoma.docentia.dto.UsuarioResponse;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.DatosInvalidosException;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.ProgramaRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Alta y edición de usuarios con su rol y su vínculo a facultad o programa. */
@Service
@Transactional
public class UsuarioService {

  private final UsuarioRepository usuarioRepository;
  private final ProgramaRepository programaRepository;
  private final FacultadService facultadService;
  private final PasswordEncoder passwordEncoder;

  public UsuarioService(
      UsuarioRepository usuarioRepository,
      ProgramaRepository programaRepository,
      FacultadService facultadService,
      PasswordEncoder passwordEncoder) {
    this.usuarioRepository = usuarioRepository;
    this.programaRepository = programaRepository;
    this.facultadService = facultadService;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Lista los usuarios, opcionalmente de un solo rol.
   *
   * @param rol rol por el que se filtra, o nulo para todos
   * @return usuarios ordenados por nombre
   */
  @Transactional(readOnly = true)
  public List<UsuarioResponse> listarUsuarios(Rol rol) {
    return usuarioRepository.findAllByOrderByNombreCompletoAsc().stream()
        .filter(usuario -> rol == null || usuario.getRol() == rol)
        .map(UsuarioResponse::desde)
        .toList();
  }

  /**
   * Obtiene un usuario.
   *
   * @param idUsuario identificador del usuario
   * @return el usuario con su alcance
   * @throws RecursoNoEncontradoException si no existe
   */
  @Transactional(readOnly = true)
  public UsuarioResponse obtenerUsuario(Long idUsuario) {
    return UsuarioResponse.desde(buscarUsuario(idUsuario));
  }

  /**
   * Crea un usuario con un único rol y el vínculo que ese rol exige.
   *
   * @param solicitud datos del usuario; la contraseña es obligatoria
   * @return el usuario creado
   * @throws DatosInvalidosException si falta la contraseña o el vínculo no corresponde al rol
   * @throws RecursoNoEncontradoException si la facultad o el programa no existen
   * @throws ReglaNegocioException si el correo ya está registrado
   */
  public UsuarioResponse crearUsuario(UsuarioRequest solicitud) {
    if (!StringUtils.hasText(solicitud.contrasena())) {
      throw new DatosInvalidosException("La contraseña es obligatoria al crear un usuario");
    }
    String correo = normalizarCorreo(solicitud.correo());
    validarCorreoDisponible(correo, null);
    Vinculo vinculo = resolverVinculo(solicitud);
    String contrasenaHash = passwordEncoder.encode(solicitud.contrasena());
    Usuario usuario =
        Usuario.crear(
            solicitud.nombreCompleto().trim(),
            correo,
            contrasenaHash,
            solicitud.rol(),
            vinculo.facultad(),
            vinculo.programa());
    // La contraseña la conoce quien creó la cuenta: el usuario la cambia al primer ingreso.
    usuario.asignarContrasenaTemporal(contrasenaHash);
    return UsuarioResponse.desde(usuarioRepository.save(usuario));
  }

  /**
   * Edita los datos, el rol y el vínculo de un usuario. Las sesiones abiertas conservan el alcance
   * anterior hasta que el token vence.
   *
   * @param idUsuario identificador del usuario
   * @param solicitud datos nuevos; si la contraseña llega vacía se conserva la actual
   * @return el usuario actualizado
   * @throws RecursoNoEncontradoException si el usuario, la facultad o el programa no existen
   * @throws DatosInvalidosException si el vínculo no corresponde al rol
   * @throws ReglaNegocioException si el correo ya lo usa otro usuario
   */
  public UsuarioResponse actualizarUsuario(Long idUsuario, UsuarioRequest solicitud) {
    Usuario usuario = buscarUsuario(idUsuario);
    String correo = normalizarCorreo(solicitud.correo());
    validarCorreoDisponible(correo, idUsuario);
    Vinculo vinculo = resolverVinculo(solicitud);
    usuario.setNombreCompleto(solicitud.nombreCompleto().trim());
    usuario.setCorreo(correo);
    usuario.asignarAlcance(solicitud.rol(), vinculo.facultad(), vinculo.programa());
    if (StringUtils.hasText(solicitud.contrasena())) {
      usuario.asignarContrasenaTemporal(passwordEncoder.encode(solicitud.contrasena()));
    }
    return UsuarioResponse.desde(usuarioRepository.saveAndFlush(usuario));
  }

  /**
   * Activa o desactiva una cuenta. Una cuenta inactiva no puede iniciar sesión; sus sesiones
   * abiertas siguen hasta que el token vence, igual que con un cambio de rol.
   *
   * @param idUsuario cuenta a cambiar
   * @param esActivo estado nuevo
   * @param idUsuarioActual quien hace el cambio
   * @return el usuario con su estado nuevo
   * @throws RecursoNoEncontradoException si el usuario no existe
   * @throws ReglaNegocioException si alguien intenta desactivar su propia cuenta
   */
  public UsuarioResponse cambiarEstado(Long idUsuario, boolean esActivo, Long idUsuarioActual) {
    Usuario usuario = buscarUsuario(idUsuario);
    if (!esActivo && idUsuario.equals(idUsuarioActual)) {
      throw new ReglaNegocioException("No puedes desactivar tu propia cuenta");
    }
    usuario.setEsActivo(esActivo);
    return UsuarioResponse.desde(usuarioRepository.saveAndFlush(usuario));
  }

  private Vinculo resolverVinculo(UsuarioRequest solicitud) {
    if (solicitud.rol() != Rol.DOCENTE) {
      Facultad facultad =
          solicitud.idFacultad() == null
              ? null
              : facultadService.buscarFacultadActiva(solicitud.idFacultad());
      Programa programa =
          solicitud.idPrograma() == null ? null : buscarProgramaActivo(solicitud.idPrograma());
      return new Vinculo(facultad, programa);
    }
    if (solicitud.idPrograma() == null) {
      throw new DatosInvalidosException("El docente debe estar vinculado a un programa");
    }
    Programa programa = buscarProgramaActivo(solicitud.idPrograma());
    // El formulario puede mandar también la facultad elegida; se acepta si es la del programa.
    if (solicitud.idFacultad() != null
        && !solicitud.idFacultad().equals(programa.getFacultad().getIdFacultad())) {
      throw new DatosInvalidosException("El programa no pertenece a la facultad indicada");
    }
    return new Vinculo(null, programa);
  }

  private Programa buscarProgramaActivo(Long idPrograma) {
    return programaRepository
        .findByIdProgramaAndEsActivoTrue(idPrograma)
        .orElseThrow(() -> new RecursoNoEncontradoException("Programa no encontrado"));
  }

  private Usuario buscarUsuario(Long idUsuario) {
    return usuarioRepository
        .findConAlcanceByIdUsuario(idUsuario)
        .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
  }

  private void validarCorreoDisponible(String correo, Long idUsuarioActual) {
    usuarioRepository
        .findByCorreoIgnoreCase(correo)
        .filter(existente -> !existente.getIdUsuario().equals(idUsuarioActual))
        .ifPresent(
            existente -> {
              throw new ReglaNegocioException("Ya existe un usuario con ese correo");
            });
  }

  private String normalizarCorreo(String correo) {
    return correo.trim().toLowerCase(Locale.ROOT);
  }

  private record Vinculo(Facultad facultad, Programa programa) {}
}
