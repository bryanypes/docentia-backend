package com.uniautonoma.docentia.service.heteroevaluacion;

import com.uniautonoma.docentia.config.HeteroevaluacionProperties;
import com.uniautonoma.docentia.dto.EnvioEstudianteRequest;
import com.uniautonoma.docentia.dto.EvaluacionesEstudianteResponse;
import com.uniautonoma.docentia.dto.EvaluacionesEstudianteResponse.Asignacion;
import com.uniautonoma.docentia.dto.FormularioEstudianteResponse;
import com.uniautonoma.docentia.dto.FormularioResponse.OpcionEscala;
import com.uniautonoma.docentia.dto.PeriodoResumenResponse;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.TipoFormulario;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.exception.NoProcesableException;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.exception.ReglaNegocioException;
import com.uniautonoma.docentia.repository.PeriodoAcademicoRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.LimitadorSolicitudes;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.CalificacionService;
import com.uniautonoma.docentia.service.PeriodoAcademicoService;
import com.uniautonoma.docentia.service.VentanaEtapaService;
import com.uniautonoma.docentia.service.formulario.CatalogoInstrumento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Heteroevaluación del estudiante. El envío escribe la participación y la respuesta en una sola
 * transacción, pero en tablas sin nada que las una: ningún rol puede saber quién respondió qué.
 */
@Service
@Transactional
public class HeteroevaluacionService {

  public static final String AVISO =
      "Tus respuestas son anónimas y se usan para la evaluación de la enseñanza. Nadie puede ver"
          + " qué respondiste.";

  private static final int DECIMALES_FUENTE = 4;
  private static final int MAXIMO_COMENTARIO = 1000;
  private static final String NO_ENCONTRADA = "Evaluación no encontrada";

  private static final String ASIGNACIONES =
      """
      select cd.id as asignacion, cu.codigo, cu.nombre as curso, c.grupo,
             u.id_usuario as id_docente, u.nombre_completo as docente, pr.nombre as programa,
             exists (select 1 from participaciones_heteroevaluacion pa
                     where pa.asignacion_id = cd.id and pa.estudiante_id = m.estudiante_id)
               as enviada
      from matriculas m
      join clases c on c.id = m.clase_id
      join clases_docente cd on cd.clase_id = c.id
      join cursos cu on cu.id = c.id_curso
      join programas pr on pr.id_programa = cu.id_programa
      join usuarios u on u.id_usuario = cd.docente_id
      where m.estudiante_id = ? and m.es_activa and c.id_periodo = ?
      """;

  private final JdbcTemplate jdbcTemplate;
  private final PeriodoAcademicoService periodoService;
  private final PeriodoAcademicoRepository periodoRepository;
  private final UsuarioRepository usuarioRepository;
  private final VentanaEtapaService ventanaEtapaService;
  private final CalificacionService calificacionService;
  private final CatalogoInstrumento catalogo;
  private final LimitadorSolicitudes limitador;
  private final HeteroevaluacionProperties propiedades;
  private final Clock reloj;
  private final List<String> etiquetasEscala;

  public HeteroevaluacionService(
      JdbcTemplate jdbcTemplate,
      PeriodoAcademicoService periodoService,
      PeriodoAcademicoRepository periodoRepository,
      UsuarioRepository usuarioRepository,
      VentanaEtapaService ventanaEtapaService,
      CalificacionService calificacionService,
      CatalogoInstrumento catalogo,
      LimitadorSolicitudes limitador,
      HeteroevaluacionProperties propiedades,
      Clock reloj,
      @Value("${docentia.formularios.etiquetas-escala}") List<String> etiquetasEscala) {
    this.jdbcTemplate = jdbcTemplate;
    this.periodoService = periodoService;
    this.periodoRepository = periodoRepository;
    this.usuarioRepository = usuarioRepository;
    this.ventanaEtapaService = ventanaEtapaService;
    this.calificacionService = calificacionService;
    this.catalogo = catalogo;
    this.limitador = limitador;
    this.propiedades = propiedades;
    this.reloj = reloj;
    this.etiquetasEscala = etiquetasEscala;
  }

  private record Fila(
      Long asignacion,
      String codigo,
      String curso,
      String grupo,
      Long idDocente,
      String docente,
      String programa,
      boolean enviada) {}

  private List<Fila> asignaciones(Long idEstudiante, Long idPeriodo, String filtro, Object... mas) {
    List<Object> parametros = new ArrayList<>(List.of(idEstudiante, idPeriodo));
    parametros.addAll(List.of(mas));
    return jdbcTemplate.query(
        ASIGNACIONES + filtro + " order by cu.nombre, c.grupo, u.nombre_completo",
        (rs, n) ->
            new Fila(
                rs.getLong("asignacion"),
                rs.getString("codigo"),
                rs.getString("curso"),
                rs.getString("grupo"),
                rs.getLong("id_docente"),
                rs.getString("docente"),
                rs.getString("programa"),
                rs.getBoolean("enviada")),
        parametros.toArray());
  }

  @Transactional(readOnly = true)
  public EvaluacionesEstudianteResponse listar(Long idPeriodo, UsuarioSesion sesion) {
    PeriodoAcademico periodo = resolverPeriodo(idPeriodo);
    List<Asignacion> lista =
        asignaciones(sesion.idUsuario(), periodo.getIdPeriodo(), "").stream()
            .map(
                fila ->
                    new Asignacion(
                        fila.asignacion(),
                        fila.codigo(),
                        fila.curso(),
                        fila.grupo(),
                        fila.docente(),
                        fila.programa(),
                        fila.enviada() ? "ENVIADA" : "PENDIENTE"))
            .toList();
    return new EvaluacionesEstudianteResponse(
        PeriodoResumenResponse.desde(periodo),
        lista.stream().filter(a -> a.estado().equals("PENDIENTE")).count(),
        lista);
  }

  @Transactional(readOnly = true)
  public FormularioEstudianteResponse leer(Long idAsignacion, UsuarioSesion sesion) {
    limitador.registrar("lectura:" + sesion.idUsuario(), propiedades.lecturasPorMinuto());
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    Fila fila = propia(idAsignacion, sesion, periodo);
    List<CatalogoInstrumento.Criterio> criterios = catalogo.de(TipoFormulario.HETERO);
    return new FormularioEstudianteResponse(
        fila.asignacion(),
        fila.curso(),
        fila.grupo(),
        fila.docente(),
        fila.enviada() ? "ENVIADA" : "PENDIENTE",
        recoleccionAbierta(periodo),
        total(criterios),
        AVISO,
        IntStream.range(0, etiquetasEscala.size())
            .mapToObj(i -> new OpcionEscala(i + 1, etiquetasEscala.get(i)))
            .toList(),
        criterios.stream()
            .map(
                criterio ->
                    new FormularioEstudianteResponse.Criterio(
                        criterio.codigo(),
                        criterio.nombre(),
                        criterio.descriptores().stream()
                            .map(
                                d ->
                                    new FormularioEstudianteResponse.Descriptor(
                                        d.codigo(), d.texto()))
                            .toList()))
            .toList());
  }

  /**
   * Envía la heteroevaluación. Es definitiva y no devuelve lo respondido.
   *
   * @throws RecursoNoEncontradoException si la asignación no es del estudiante o está retirado
   * @throws ReglaNegocioException si ya la envió o la recolección está cerrada
   * @throws NoProcesableException si falta un ítem o un valor es inválido
   */
  public void enviar(Long idAsignacion, EnvioEstudianteRequest solicitud, UsuarioSesion sesion) {
    limitador.registrar("envio:" + sesion.idUsuario(), propiedades.enviosPorMinuto());
    PeriodoAcademico periodo = periodoService.exigirPeriodoActivo();
    Fila fila = propia(idAsignacion, sesion, periodo);
    ventanaEtapaService.exigirEtapaAbierta(periodo, TipoEtapa.RECOLECCION_CALIFICACIONES);
    Map<Long, Short> valores = validar(solicitud);
    String comentario = comentario(solicitud.comentario());

    // Toda escritura sobre las respuestas de un docente pasa en fila: así el doble clic y el
    // recálculo concurrente no se pisan.
    jdbcTemplate.queryForObject(
        "select id_usuario from usuarios where id_usuario = ? for update",
        Long.class,
        fila.idDocente());
    int nueva =
        jdbcTemplate.update(
            "insert into participaciones_heteroevaluacion (asignacion_id, estudiante_id, enviada_en)"
                + " values (?, ?, ?) on conflict do nothing",
            idAsignacion,
            sesion.idUsuario(),
            Timestamp.valueOf(LocalDateTime.now(reloj)));
    if (nueva == 0) {
      throw new ReglaNegocioException("Ya enviaste esta evaluación");
    }
    UUID respuesta =
        jdbcTemplate.queryForObject(
            "insert into respuestas_heteroevaluacion"
                + " (asignacion_id, docente_id, periodo_id, instrumento_version, comentario)"
                + " values (?, ?, ?, ?, ?) returning id",
            UUID.class,
            idAsignacion,
            fila.idDocente(),
            periodo.getIdPeriodo(),
            CatalogoInstrumento.VERSION,
            comentario);
    jdbcTemplate.batchUpdate(
        "insert into respuestas_descriptor_hetero (respuesta_id, descriptor_id, valor)"
            + " values (?, ?, ?)",
        valores.entrySet().stream()
            .map(valor -> new Object[] {respuesta, valor.getKey(), valor.getValue()})
            .toList());
    recalcular(usuarioRepository.getReferenceById(fila.idDocente()), periodo);
  }

  /**
   * Con el mínimo de respuestas, la media de las medias por respuesta se vuelve la nota de
   * estudiantes y prevalece sobre el CSV. Por debajo del mínimo no se usa ni se muestra nada.
   */
  public void recalcular(Usuario docente, PeriodoAcademico periodo) {
    Map<String, Object> agregado =
        jdbcTemplate.queryForMap(
            """
            select count(*) as respuestas, avg(media) as media
            from (select r.id, avg(d.valor::numeric) as media
                  from respuestas_heteroevaluacion r
                  join respuestas_descriptor_hetero d on d.respuesta_id = r.id
                  where r.docente_id = ? and r.periodo_id = ?
                  group by r.id) por_respuesta
            """,
            docente.getIdUsuario(),
            periodo.getIdPeriodo());
    long respuestas = ((Number) agregado.get("respuestas")).longValue();
    if (respuestas < propiedades.minimoRespuestas()) {
      return;
    }
    BigDecimal media =
        ((BigDecimal) agregado.get("media")).setScale(DECIMALES_FUENTE, RoundingMode.HALF_UP);
    calificacionService.registrarDesdeFormulario(
        docente, periodo, FuenteEvaluacion.ESTUDIANTES, media, null);
  }

  private Fila propia(Long idAsignacion, UsuarioSesion sesion, PeriodoAcademico periodo) {
    return asignaciones(sesion.idUsuario(), periodo.getIdPeriodo(), " and cd.id = ?", idAsignacion)
        .stream()
        .findFirst()
        .orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADA));
  }

  private Map<Long, Short> validar(EnvioEstudianteRequest solicitud) {
    Map<String, Long> descriptores = new HashMap<>();
    catalogo
        .de(TipoFormulario.HETERO)
        .forEach(c -> c.descriptores().forEach(d -> descriptores.put(d.codigo(), d.id())));
    Map<String, BigDecimal> respuestas =
        solicitud.respuestas() == null ? Map.of() : solicitud.respuestas();
    Map<Long, Short> valores = new HashMap<>();
    respuestas.forEach(
        (codigo, valor) -> {
          Long id = descriptores.get(codigo);
          if (id == null) {
            throw new NoProcesableException(
                "El ítem " + codigo + " no pertenece a este formulario");
          }
          if (valor == null
              || valor.stripTrailingZeros().scale() > 0
              || valor.compareTo(BigDecimal.ONE) < 0
              || valor.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new NoProcesableException("Cada ítem se califica con un entero de 1 a 5");
          }
          valores.put(id, valor.shortValue());
        });
    if (valores.size() < descriptores.size()) {
      throw new NoProcesableException(
          "Faltan "
              + (descriptores.size() - valores.size())
              + " de "
              + descriptores.size()
              + " ítems por responder");
    }
    return valores;
  }

  private static String comentario(String texto) {
    if (texto == null || texto.isBlank()) {
      return null;
    }
    if (texto.length() > MAXIMO_COMENTARIO) {
      throw new NoProcesableException(
          "El comentario no puede superar los " + MAXIMO_COMENTARIO + " caracteres");
    }
    return texto.trim();
  }

  private static int total(List<CatalogoInstrumento.Criterio> criterios) {
    return criterios.stream().mapToInt(c -> c.descriptores().size()).sum();
  }

  private PeriodoAcademico resolverPeriodo(Long idPeriodo) {
    return idPeriodo == null
        ? periodoService.exigirPeriodoActivo()
        : periodoRepository
            .findById(idPeriodo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Periodo académico no encontrado"));
  }

  private boolean recoleccionAbierta(PeriodoAcademico periodo) {
    return ventanaEtapaService.describirVentanas(periodo).stream()
        .anyMatch(v -> v.etapa() == TipoEtapa.RECOLECCION_CALIFICACIONES && v.abierta());
  }
}
