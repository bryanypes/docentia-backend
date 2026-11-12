package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.config.NotificacionProperties;
import com.uniautonoma.docentia.exception.DatosInvalidosException;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.LimitadorSolicitudes;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recuperación de contraseña sin sesión. La solicitud siempre responde lo mismo, exista o no el
 * correo. El token viaja solo en el correo; en la base queda su SHA-256, vence en 30 minutos y
 * sirve una vez. Ni el token ni la contraseña van al log.
 */
@Service
public class RecuperacionContrasenaService {

  public static final String MENSAJE_GENERICO =
      "Si el correo está registrado, te enviamos un enlace para restablecer la contraseña. Revisa tu"
          + " bandeja en unos minutos.";
  static final Duration VIGENCIA = Duration.ofMinutes(30);
  static final int SOLICITUDES_POR_MINUTO_IP = 5;
  static final int SOLICITUDES_POR_MINUTO_CORREO = 3;
  static final int RESTABLECIMIENTOS_POR_MINUTO_IP = 10;
  private static final String ENLACE_INVALIDO =
      "El enlace no es válido o ya venció. Pide uno nuevo desde «¿Olvidaste tu contraseña?».";

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final JdbcTemplate jdbcTemplate;
  private final LimitadorSolicitudes limitador;
  private final NotificacionProperties notificaciones;
  private final Clock reloj;
  private final SecureRandom aleatorio = new SecureRandom();

  public RecuperacionContrasenaService(
      UsuarioRepository usuarioRepository,
      PasswordEncoder passwordEncoder,
      JdbcTemplate jdbcTemplate,
      LimitadorSolicitudes limitador,
      NotificacionProperties notificaciones,
      Clock reloj) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.jdbcTemplate = jdbcTemplate;
    this.limitador = limitador;
    this.notificaciones = notificaciones;
    this.reloj = reloj;
  }

  /** Registra la solicitud y, solo si el correo es de un usuario activo, encola el enlace. */
  @Transactional
  public void solicitar(String correo, String ip) {
    String normalizado = correo == null ? "" : correo.strip().toLowerCase(Locale.ROOT);
    limitador.registrar("recuperacion-ip:" + ip, SOLICITUDES_POR_MINUTO_IP);
    limitador.registrar("recuperacion-correo:" + normalizado, SOLICITUDES_POR_MINUTO_CORREO);
    String token = nuevoToken();
    String hash = sha256(token);
    usuarioRepository
        .findByCorreoIgnoreCaseAndEsActivoTrue(normalizado)
        .ifPresent(
            usuario -> {
              LocalDateTime ahora = LocalDateTime.now(reloj);
              jdbcTemplate.update(
                  "update recuperaciones_contrasena set usado_en = ? where id_usuario = ? and usado_en is null",
                  Timestamp.valueOf(ahora),
                  usuario.getIdUsuario());
              jdbcTemplate.update(
                  "insert into recuperaciones_contrasena (id_usuario, hash_token, vence_en) values (?, ?, ?)",
                  usuario.getIdUsuario(),
                  hash,
                  Timestamp.valueOf(ahora.plus(VIGENCIA)));
              jdbcTemplate.update(
                  "insert into cola_correo (destinatario, asunto, cuerpo) values (?, ?, ?)",
                  usuario.getCorreo(),
                  "DOCENTIA: Restablecer tu contraseña",
                  cuerpo(usuario.getNombreCompleto(), token));
            });
  }

  /**
   * Cambia la contraseña con un token vigente y lo invalida.
   *
   * @throws DatosInvalidosException si el token no existe, ya se usó o venció
   */
  @Transactional
  public void restablecer(String token, String contrasena, String ip) {
    limitador.registrar("restablecimiento-ip:" + ip, RESTABLECIMIENTOS_POR_MINUTO_IP);
    LocalDateTime ahora = LocalDateTime.now(reloj);
    List<Long> usuarios =
        jdbcTemplate.queryForList(
            "select id_usuario from recuperaciones_contrasena where hash_token = ? and usado_en is null"
                + " and vence_en > ? for update",
            Long.class,
            sha256(token == null ? "" : token.strip()),
            Timestamp.valueOf(ahora));
    if (usuarios.isEmpty()) {
      throw new DatosInvalidosException(ENLACE_INVALIDO);
    }
    Long idUsuario = usuarios.get(0);
    int cambiados =
        jdbcTemplate.update(
            "update usuarios set contrasena_hash = ?, debe_cambiar_contrasena = false"
                + " where id_usuario = ? and es_activo",
            passwordEncoder.encode(contrasena),
            idUsuario);
    if (cambiados == 0) {
      throw new DatosInvalidosException(ENLACE_INVALIDO);
    }
    jdbcTemplate.update(
        "update recuperaciones_contrasena set usado_en = ? where id_usuario = ? and usado_en is null",
        Timestamp.valueOf(ahora),
        idUsuario);
  }

  private String cuerpo(String nombre, String token) {
    String base = notificaciones.urlAplicacion().replaceAll("/+$", "");
    return "Hola, "
        + nombre
        + ".\n\nPara crear una nueva contraseña en DOCENTIA abre este enlace. Vence en 30 minutos y"
        + " sirve una sola vez:\n\n"
        + base
        + "/restablecer-contrasena?token="
        + token
        + "\n\nSi no lo pediste, ignora este correo: tu contraseña no cambia.";
  }

  private String nuevoToken() {
    byte[] bytes = new byte[32];
    aleatorio.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  static String sha256(String texto) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException imposible) {
      throw new IllegalStateException(imposible);
    }
  }
}
