package com.uniautonoma.docentia.service.notificacion;

import com.uniautonoma.docentia.config.NotificacionProperties;
import com.uniautonoma.docentia.dto.NotificacionResponse;
import com.uniautonoma.docentia.dto.PaginaResponse;
import com.uniautonoma.docentia.dto.PreferenciasNotificacionResponse;
import com.uniautonoma.docentia.entity.Notificacion;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.NotificacionRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Crea avisos y atiende la bandeja. Crear un aviso nunca propaga un error: cada destinatario va en
 * su propia transacción, y si falla se registra y el negocio sigue.
 */
@Service
public class NotificacionService {

  public static final int TAMANO_MAXIMO = 50;
  private static final Logger LOG = LoggerFactory.getLogger(NotificacionService.class);

  private static final String INSERTAR =
      """
      insert into notificaciones
          (usuario_id, tipo, titulo, mensaje, enlace, entidad_tipo, entidad_id, clave_dedupe)
      values (?, ?, ?, ?, ?, ?, ?, ?)
      on conflict (usuario_id, tipo, clave_dedupe) do nothing
      returning id
      """;
  private static final String ENCOLAR =
      """
      insert into cola_correo (notificacion_id, destinatario, asunto, cuerpo)
      select ?, ?, ?, ?
      where coalesce(
          (select correo_habilitado from preferencias_notificacion where usuario_id = ?), true)
      """;
  private static final String GUARDAR_PREFERENCIA =
      """
      insert into preferencias_notificacion (usuario_id, correo_habilitado) values (?, ?)
      on conflict (usuario_id) do update set correo_habilitado = excluded.correo_habilitado
      """;

  private final NotificacionRepository notificacionRepository;
  private final UsuarioRepository usuarioRepository;
  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transaccionNueva;
  private final PlantillaCorreo plantilla;
  private final Clock reloj;

  public NotificacionService(
      NotificacionRepository notificacionRepository,
      UsuarioRepository usuarioRepository,
      JdbcTemplate jdbcTemplate,
      PlatformTransactionManager transacciones,
      NotificacionProperties propiedades,
      Clock reloj) {
    this.notificacionRepository = notificacionRepository;
    this.usuarioRepository = usuarioRepository;
    this.jdbcTemplate = jdbcTemplate;
    this.transaccionNueva = new TransactionTemplate(transacciones);
    this.transaccionNueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.plantilla = new PlantillaCorreo(propiedades.urlAplicacion());
    this.reloj = reloj;
  }

  /** Avisa a cada destinatario del evento. Nunca lanza una excepción. */
  public void notificar(EventoNotificacion evento) {
    try {
      resolverDestinatarios(evento).forEach(usuario -> notificarA(usuario, evento));
    } catch (RuntimeException fallo) {
      LOG.error("No se pudo resolver el aviso {}: {}", evento.tipo(), fallo.getClass().getName());
    }
  }

  private void notificarA(Usuario usuario, EventoNotificacion evento) {
    try {
      transaccionNueva.executeWithoutResult(estado -> crear(usuario, evento));
    } catch (RuntimeException fallo) {
      LOG.error(
          "No se pudo crear el aviso {} para el usuario {}: {}",
          evento.tipo(),
          usuario.getIdUsuario(),
          fallo.getClass().getName());
    }
  }

  private void crear(Usuario usuario, EventoNotificacion evento) {
    String mensaje = plantilla.mensaje(evento);
    List<Long> creada =
        jdbcTemplate.queryForList(
            INSERTAR,
            Long.class,
            usuario.getIdUsuario(),
            evento.tipo().name(),
            evento.tipo().getTitulo(),
            mensaje,
            evento.enlace(),
            evento.entidadTipo(),
            evento.entidadId(),
            evento.claveDedupe());
    if (creada.isEmpty()) {
      return;
    }
    jdbcTemplate.update(
        ENCOLAR,
        creada.get(0),
        usuario.getCorreo(),
        plantilla.asunto(evento),
        plantilla.cuerpo(usuario, mensaje, evento.enlace()),
        usuario.getIdUsuario());
  }

  private List<Usuario> resolverDestinatarios(EventoNotificacion evento) {
    return switch (evento.destino()) {
      case USUARIO ->
          usuarioRepository.findById(evento.idDestino()).filter(Usuario::getEsActivo).stream()
              .toList();
      case COORDINADORES_FACULTAD ->
          usuarioRepository.buscarCoordinadoresActivos(evento.idDestino());
      case VICERRECTORES ->
          usuarioRepository.findByRolAndEsActivoTrueOrderByNombreCompletoAsc(Rol.VICERRECTOR);
    };
  }

  @Transactional(readOnly = true)
  public PaginaResponse<NotificacionResponse> listar(
      Long idUsuario, boolean soloNoLeidas, int pagina, int tamano) {
    Pageable solicitud =
        PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), TAMANO_MAXIMO));
    return PaginaResponse.desde(
        soloNoLeidas
            ? notificacionRepository.findByIdUsuarioAndLeidaOrderByCreadaEnDescIdDesc(
                idUsuario, false, solicitud)
            : notificacionRepository.findByIdUsuarioOrderByCreadaEnDescIdDesc(idUsuario, solicitud),
        NotificacionResponse::desde);
  }

  @Transactional(readOnly = true)
  public long contarNoLeidas(Long idUsuario) {
    return notificacionRepository.countByIdUsuarioAndLeidaFalse(idUsuario);
  }

  /**
   * Marca un aviso propio como leído.
   *
   * @throws RecursoNoEncontradoException si no existe o es de otro usuario
   */
  @Transactional
  public NotificacionResponse marcarLeida(Long id, Long idUsuario) {
    Notificacion notificacion =
        notificacionRepository
            .findByIdAndIdUsuario(id, idUsuario)
            .orElseThrow(() -> new RecursoNoEncontradoException("Notificación no encontrada"));
    notificacion.marcarLeida(LocalDateTime.now(reloj));
    return NotificacionResponse.desde(notificacion);
  }

  @Transactional
  public void marcarTodasLeidas(Long idUsuario) {
    notificacionRepository.marcarTodasLeidas(idUsuario, LocalDateTime.now(reloj));
  }

  @Transactional(readOnly = true)
  public PreferenciasNotificacionResponse leerPreferencias(Long idUsuario) {
    List<Boolean> guardada =
        jdbcTemplate.queryForList(
            "select correo_habilitado from preferencias_notificacion where usuario_id = ?",
            Boolean.class,
            idUsuario);
    return new PreferenciasNotificacionResponse(guardada.isEmpty() || guardada.get(0));
  }

  @Transactional
  public PreferenciasNotificacionResponse guardarPreferencias(Long idUsuario, boolean correo) {
    jdbcTemplate.update(GUARDAR_PREFERENCIA, idUsuario, correo);
    return new PreferenciasNotificacionResponse(correo);
  }
}
