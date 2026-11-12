package com.uniautonoma.docentia.config;

import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Crea el primer ADMINISTRADOR en un ambiente sin seed (todo lo que no es {@code dev}). Solo la
 * administración crea usuarios, así que sin esto una base nueva o una sin administrador no tiene
 * con quién entrar.
 *
 * <p>Actúa una sola vez: con {@code DOCENTIA_BOOTSTRAP_EMAIL} definido y ningún administrador
 * activo. Si ya hay uno no toca nada. La contraseña, si se da, queda temporal y hay que cambiarla
 * al entrar; sin contraseña la cuenta solo entra con Google. Ni la contraseña ni el correo van al
 * log. Ver docs/OPERACION.md.
 */
@Component
@Profile("!dev")
public class PrimerUsuario implements ApplicationRunner {

  static final String DOMINIO = "@uniautonoma.edu.co";
  static final int CONTRASENA_MINIMA = 12;
  static final int CONTRASENA_MAXIMA = 72;
  private static final Pattern CORREO = Pattern.compile("^[a-z0-9._%+-]+@uniautonoma\\.edu\\.co$");

  /** Llave del candado de Postgres: dos réplicas que arrancan a la vez no crean dos usuarios. */
  private static final long LLAVE_CANDADO = 740_2026_001L;

  private static final Logger LOG = LoggerFactory.getLogger(PrimerUsuario.class);

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transaccion;
  private final GoogleProperties google;
  private final String correo;
  private final String nombre;
  private final String contrasena;

  public PrimerUsuario(
      UsuarioRepository usuarioRepository,
      PasswordEncoder passwordEncoder,
      JdbcTemplate jdbcTemplate,
      TransactionTemplate transaccion,
      GoogleProperties google,
      @Value("${docentia.bootstrap.email:}") String correo,
      @Value("${docentia.bootstrap.nombre:}") String nombre,
      @Value("${docentia.bootstrap.contrasena:}") String contrasena) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.jdbcTemplate = jdbcTemplate;
    this.transaccion = transaccion;
    this.google = google;
    this.correo = correo == null ? "" : correo.strip().toLowerCase(Locale.ROOT);
    this.nombre = nombre == null ? "" : nombre.strip();
    this.contrasena = contrasena == null ? "" : contrasena;
  }

  @Override
  public void run(ApplicationArguments argumentos) {
    crearSiHaceFalta();
  }

  /**
   * Crea el usuario si hay configuración y la tabla está vacía.
   *
   * @return si lo creó
   * @throws IllegalStateException si la configuración está incompleta o no es válida; la app no
   *     arranca, para que el error se vea en el despliegue y no al intentar entrar
   */
  public boolean crearSiHaceFalta() {
    if (correo.isEmpty()) {
      return false;
    }
    validar();
    Boolean creado =
        transaccion.execute(
            estado -> {
              jdbcTemplate.query("select pg_advisory_xact_lock(?)", fila -> null, LLAVE_CANDADO);
              Long administradores =
                  jdbcTemplate.queryForObject(
                      "select count(*) from usuarios where rol = 'ADMINISTRADOR' and es_activo",
                      Long.class);
              if (administradores != null && administradores > 0) {
                LOG.warn(
                    "DOCENTIA_BOOTSTRAP_EMAIL está definido pero ya hay un administrador: no se crea nada."
                        + " Quita las variables DOCENTIA_BOOTSTRAP_* del entorno.");
                return false;
              }
              Usuario vicerrector =
                  Usuario.crear(
                      nombre,
                      correo,
                      passwordEncoder.encode(claveInicial()),
                      Rol.ADMINISTRADOR,
                      null,
                      null);
              if (!contrasena.isEmpty()) {
                vicerrector.asignarContrasenaTemporal(vicerrector.getContrasenaHash());
              }
              Long id = usuarioRepository.save(vicerrector).getIdUsuario();
              LOG.warn(
                  "Se creó el primer administrador (id {}), {}. Quita las variables"
                      + " DOCENTIA_BOOTSTRAP_* del entorno.",
                  id,
                  contrasena.isEmpty()
                      ? "sin contraseña: entra con Google"
                      : "con contraseña temporal: debe cambiarla al entrar");
              return true;
            });
    return Boolean.TRUE.equals(creado);
  }

  /** Sin contraseña se guarda un hash de un valor aleatorio que nadie conoce. */
  private String claveInicial() {
    return contrasena.isEmpty() ? UUID.randomUUID().toString() + UUID.randomUUID() : contrasena;
  }

  void validar() {
    if (!CORREO.matcher(correo).matches()) {
      throw new IllegalStateException(
          "DOCENTIA_BOOTSTRAP_EMAIL debe ser un correo institucional que termine en " + DOMINIO);
    }
    if (nombre.isEmpty() || nombre.length() > 150) {
      throw new IllegalStateException(
          "DOCENTIA_BOOTSTRAP_NOMBRE es obligatorio y tiene máximo 150 caracteres");
    }
    if (contrasena.isEmpty()) {
      if (!google.habilitado()) {
        throw new IllegalStateException(
            "Sin DOCENTIA_BOOTSTRAP_CONTRASENA el primer usuario solo puede entrar con Google, y"
                + " DOCENTIA_GOOGLE_CLIENT_ID no está configurado");
      }
      return;
    }
    if (contrasena.length() < CONTRASENA_MINIMA || contrasena.length() > CONTRASENA_MAXIMA) {
      throw new IllegalStateException(
          "DOCENTIA_BOOTSTRAP_CONTRASENA debe tener entre "
              + CONTRASENA_MINIMA
              + " y "
              + CONTRASENA_MAXIMA
              + " caracteres");
    }
  }
}
