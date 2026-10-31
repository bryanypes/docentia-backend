package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.PaginaResponse;
import com.uniautonoma.docentia.dto.RegistroAuditoriaResponse;
import com.uniautonoma.docentia.entity.AccionAuditoria;
import com.uniautonoma.docentia.security.DireccionCliente;
import com.uniautonoma.docentia.security.UsuarioSesion;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Escribe y consulta la auditoría. Escribe en su propia transacción, para que un intento fallido
 * también quede, y un fallo aquí no detiene el negocio. No guarda datos de la evaluación.
 */
@Service
public class AuditoriaService {

  public static final String EXITO = "EXITO";
  public static final String FALLO = "FALLO";
  private static final int TAMANO_MAXIMO = 50;
  private static final Logger LOG = LoggerFactory.getLogger(AuditoriaService.class);

  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transaccionNueva;

  public AuditoriaService(JdbcTemplate jdbcTemplate, PlatformTransactionManager transacciones) {
    this.jdbcTemplate = jdbcTemplate;
    this.transaccionNueva = new TransactionTemplate(transacciones);
    this.transaccionNueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public void registrar(
      AccionAuditoria accion, String entidad, Long idEntidad, UsuarioSesion sesion) {
    registrar(
        accion,
        entidad,
        idEntidad,
        sesion.idUsuario(),
        sesion.correo(),
        sesion.rol().name(),
        EXITO);
  }

  public void registrar(
      AccionAuditoria accion,
      String entidad,
      Long idEntidad,
      Long idUsuario,
      String correo,
      String rol,
      String resultado) {
    try {
      transaccionNueva.executeWithoutResult(
          estado ->
              jdbcTemplate.update(
                  "insert into auditoria"
                      + " (usuario_id, correo, rol, accion, entidad, entidad_id, ip, resultado)"
                      + " values (?, ?, ?, ?, ?, ?, ?, ?)",
                  idUsuario,
                  correo,
                  rol,
                  accion.name(),
                  entidad,
                  idEntidad,
                  ipActual(),
                  resultado));
    } catch (RuntimeException fallo) {
      LOG.error("No se pudo auditar {}: {}", accion, fallo.getClass().getName());
    }
  }

  private static String ipActual() {
    return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes actual
        ? DireccionCliente.de(actual.getRequest())
        : null;
  }

  /** Consulta paginada, de lo más reciente a lo más antiguo. */
  public PaginaResponse<RegistroAuditoriaResponse> consultar(
      LocalDate desde,
      LocalDate hasta,
      AccionAuditoria accion,
      String usuario,
      int pagina,
      int tamano) {
    StringBuilder filtro = new StringBuilder(" where 1 = 1");
    List<Object> parametros = new ArrayList<>();
    if (desde != null) {
      filtro.append(" and fecha >= ?");
      parametros.add(Timestamp.valueOf(desde.atStartOfDay()));
    }
    if (hasta != null) {
      filtro.append(" and fecha < ?");
      parametros.add(Timestamp.valueOf(hasta.plusDays(1).atStartOfDay()));
    }
    if (accion != null) {
      filtro.append(" and accion = ?");
      parametros.add(accion.name());
    }
    if (usuario != null && !usuario.isBlank()) {
      filtro.append(" and correo ilike ? escape '!'");
      parametros.add(
          "%" + usuario.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
    }
    long total =
        jdbcTemplate.queryForObject(
            "select count(*) from auditoria" + filtro, Long.class, parametros.toArray());
    int tamanoReal = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
    int paginaReal = Math.max(pagina, 0);
    List<Object> conPagina = new ArrayList<>(parametros);
    conPagina.add(tamanoReal);
    conPagina.add((long) paginaReal * tamanoReal);
    List<RegistroAuditoriaResponse> filas =
        jdbcTemplate.query(
            "select id, correo, rol, accion, entidad, entidad_id, fecha, ip, resultado"
                + " from auditoria"
                + filtro
                + " order by fecha desc, id desc limit ? offset ?",
            (fila, numero) ->
                new RegistroAuditoriaResponse(
                    fila.getLong("id"),
                    fila.getString("correo"),
                    fila.getString("rol"),
                    fila.getString("accion"),
                    fila.getString("entidad"),
                    (Long) fila.getObject("entidad_id"),
                    fila.getTimestamp("fecha").toLocalDateTime(),
                    fila.getString("ip"),
                    fila.getString("resultado")),
            conPagina.toArray());
    return new PaginaResponse<>(
        filas, paginaReal, tamanoReal, total, (int) Math.ceil((double) total / tamanoReal));
  }
}
