package com.uniautonoma.docentia.service.analisis;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Registro del gasto del proveedor externo y caché de sus respuestas. Aquí nunca entra texto de
 * comentarios: solo conteos, tokens, la huella de la entrada y el resultado.
 */
@Repository
public class UsoIaRepositorio {

  /** Una fila de uso. */
  public record Registro(
      Long idPeriodo,
      String proveedor,
      String modelo,
      String versionPrompt,
      String huella,
      int comentariosDisponibles,
      int comentariosEnviados,
      int tokensEntrada,
      int tokensSalida,
      int tokensCache,
      int tokensEntradaSinTopes,
      boolean estimado,
      String resultado,
      long duracionMs) {}

  /** Respuesta guardada del proveedor. */
  public record EnCache(String contenido, String modelo, int tokensEntrada, int tokensSalida) {}

  /** Agregado para el endpoint de uso. */
  public record Agregado(
      long llamadas,
      long aciertosCache,
      long rechazados,
      long errores,
      long tokensEntrada,
      long tokensSalida,
      long tokensCache,
      long tokensAhorradosCache,
      long tokensEntradaSinTopes,
      long tokensEntradaConTopes,
      long estimados) {}

  private final JdbcTemplate jdbcTemplate;
  private final Clock reloj;

  public UsoIaRepositorio(JdbcTemplate jdbcTemplate, Clock reloj) {
    this.jdbcTemplate = jdbcTemplate;
    this.reloj = reloj;
  }

  public void registrar(Registro r) {
    jdbcTemplate.update(
        "insert into uso_ia (fecha, id_periodo, proveedor, modelo, version_prompt, huella,"
            + " comentarios_disponibles, comentarios_enviados, tokens_entrada, tokens_salida,"
            + " tokens_cache, tokens_entrada_sin_topes, estimado, resultado, duracion_ms)"
            + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        LocalDateTime.now(reloj),
        r.idPeriodo(),
        recortar(r.proveedor(), 40),
        recortar(r.modelo(), 100),
        recortar(r.versionPrompt(), 40),
        r.huella(),
        r.comentariosDisponibles(),
        r.comentariosEnviados(),
        r.tokensEntrada(),
        r.tokensSalida(),
        r.tokensCache(),
        r.tokensEntradaSinTopes(),
        r.estimado(),
        r.resultado(),
        (int) Math.min(Integer.MAX_VALUE, r.duracionMs()));
  }

  public Optional<EnCache> buscar(String huella) {
    return jdbcTemplate
        .query(
            "select contenido, modelo, tokens_entrada, tokens_salida from cache_respuestas_ia"
                + " where huella = ?",
            (fila, n) ->
                new EnCache(fila.getString(1), fila.getString(2), fila.getInt(3), fila.getInt(4)),
            huella)
        .stream()
        .findFirst();
  }

  /** Guarda la respuesta; si otra corrida ya la guardó con la misma huella, la deja. */
  public void guardar(
      String huella,
      String modelo,
      String versionPrompt,
      String contenido,
      int tokensEntrada,
      int tokensSalida) {
    jdbcTemplate.update(
        "insert into cache_respuestas_ia"
            + " (huella, modelo, version_prompt, contenido, tokens_entrada, tokens_salida)"
            + " values (?, ?, ?, ?, ?, ?) on conflict (huella) do nothing",
        huella,
        recortar(modelo, 100),
        recortar(versionPrompt, 40),
        contenido,
        tokensEntrada,
        tokensSalida);
  }

  /** Tokens gastados (entrada + salida) en llamadas reales de un día. */
  public long tokensDelDia(LocalDate dia) {
    Long total =
        jdbcTemplate.queryForObject(
            "select coalesce(sum(tokens_entrada + tokens_salida), 0) from uso_ia"
                + " where resultado = 'LLAMADA' and fecha >= ? and fecha < ?",
            Long.class,
            dia.atStartOfDay(),
            dia.plusDays(1).atStartOfDay());
    return total == null ? 0 : total;
  }

  /** Tokens gastados (entrada + salida) en llamadas reales de un periodo académico. */
  public long tokensDelPeriodo(Long idPeriodo) {
    Long total =
        jdbcTemplate.queryForObject(
            "select coalesce(sum(tokens_entrada + tokens_salida), 0) from uso_ia"
                + " where resultado = 'LLAMADA' and id_periodo = ?",
            Long.class,
            idPeriodo);
    return total == null ? 0 : total;
  }

  public Agregado agregado(Long idPeriodo) {
    return jdbcTemplate.queryForObject(
        """
        select
          count(*) filter (where resultado = 'LLAMADA'),
          count(*) filter (where resultado = 'ACIERTO_CACHE'),
          count(*) filter (where resultado = 'RECHAZADO_PRESUPUESTO'),
          count(*) filter (where resultado = 'ERROR'),
          coalesce(sum(tokens_entrada) filter (where resultado = 'LLAMADA'), 0),
          coalesce(sum(tokens_salida) filter (where resultado = 'LLAMADA'), 0),
          coalesce(sum(tokens_cache) filter (where resultado = 'LLAMADA'), 0),
          coalesce(sum(tokens_entrada + tokens_salida) filter (where resultado = 'ACIERTO_CACHE'), 0),
          coalesce(sum(tokens_entrada_sin_topes) filter (where resultado = 'LLAMADA'), 0),
          coalesce(sum(tokens_entrada) filter (where resultado = 'LLAMADA'), 0),
          count(*) filter (where estimado and resultado = 'LLAMADA')
        from uso_ia where id_periodo = ?
        """,
        (f, n) ->
            new Agregado(
                f.getLong(1),
                f.getLong(2),
                f.getLong(3),
                f.getLong(4),
                f.getLong(5),
                f.getLong(6),
                f.getLong(7),
                f.getLong(8),
                f.getLong(9),
                f.getLong(10),
                f.getLong(11)),
        idPeriodo);
  }

  private static String recortar(String texto, int maximo) {
    if (texto == null) {
      return "";
    }
    return texto.length() <= maximo ? texto : texto.substring(0, maximo);
  }
}
