package com.uniautonoma.docentia.service.analisis;

import com.uniautonoma.docentia.config.AnalisisComentariosProperties;
import com.uniautonoma.docentia.config.HeteroevaluacionProperties;
import com.uniautonoma.docentia.config.IaProperties;
import com.uniautonoma.docentia.entity.CitaHallazgo;
import com.uniautonoma.docentia.entity.EstadoSolicitudAnalisis;
import com.uniautonoma.docentia.entity.FuenteComentarios;
import com.uniautonoma.docentia.entity.Hallazgo;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.TipoNotificacion;
import com.uniautonoma.docentia.repository.SolicitudAnalisisRepository;
import com.uniautonoma.docentia.service.VentanaEtapaService;
import com.uniautonoma.docentia.service.analisis.LectorRespuestaAnalisis.HallazgoPropuesto;
import com.uniautonoma.docentia.service.analisis.LectorRespuestaAnalisis.RespuestaInvalidaException;
import com.uniautonoma.docentia.service.analisis.ModeloIA.ModeloIAException;
import com.uniautonoma.docentia.service.analisis.ModeloIA.PeticionAnalisis;
import com.uniautonoma.docentia.service.analisis.ModeloIA.RespuestaModelo;
import com.uniautonoma.docentia.service.analisis.ResultadosAnalisisRepositorio.ComentarioGuardado;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Comentario;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Etiquetado;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Salida;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorLocal;
import com.uniautonoma.docentia.service.analisis.local.PipelineAnalisis;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Advertencia;
import com.uniautonoma.docentia.service.notificacion.EventoNotificacion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Procesa la cola de solicitudes de análisis. Cada solicitud pasa por tres pasos con transacciones
 * cortas, para no tener una conexión abierta mientras el modelo responde:
 *
 * <ol>
 *   <li>tomar la siguiente EN_COLA y pasarla a EN_PROCESO (con SKIP LOCKED, así dos procesadores
 *       nunca toman la misma);
 *   <li>preparar los comentarios: contar encuestas (si no alcanzan, INSUFICIENTE), excluir los
 *       ofensivos o sobre la persona del docente y anonimizar el resto;
 *   <li>llamar al modelo fuera de la transacción, validar el JSON (un reintento) y guardar.
 * </ol>
 */
@Component
public class ProcesadorSolicitudesAnalisis {

  private final ApplicationEventPublisher eventos;

  static final String MENSAJE_ERROR =
      "No fue posible generar el análisis en este momento. Puedes volver a solicitarlo más tarde.";

  private static final Logger LOG = LoggerFactory.getLogger(ProcesadorSolicitudesAnalisis.class);

  private final SolicitudAnalisisRepository solicitudRepository;
  private final ProveedoresComentarios proveedores;
  private final PreparadorComentarios preparador;
  private final PipelineAnalisis pipeline;
  private final AnalizadorLocal analizador;
  private final ResultadosAnalisisRepositorio resultados;
  private final AnalisisComentariosProperties analisis;
  private final HeteroevaluacionProperties heteroevaluacion;
  private final VentanaEtapaService ventanas;
  private final InstruccionAnalisis instruccion;
  private final ModeloIA modelo;
  private final LectorRespuestaAnalisis lector;
  private final IaProperties propiedades;
  private final TransactionTemplate transaccion;
  private final JdbcTemplate jdbcTemplate;

  public ProcesadorSolicitudesAnalisis(
      SolicitudAnalisisRepository solicitudRepository,
      ProveedoresComentarios proveedores,
      PreparadorComentarios preparador,
      PipelineAnalisis pipeline,
      AnalizadorLocal analizador,
      ResultadosAnalisisRepositorio resultados,
      AnalisisComentariosProperties analisis,
      HeteroevaluacionProperties heteroevaluacion,
      VentanaEtapaService ventanas,
      InstruccionAnalisis instruccion,
      ModeloIA modelo,
      LectorRespuestaAnalisis lector,
      IaProperties propiedades,
      TransactionTemplate transaccion,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher eventos) {
    this.eventos = eventos;
    this.solicitudRepository = solicitudRepository;
    this.proveedores = proveedores;
    this.preparador = preparador;
    this.pipeline = pipeline;
    this.analizador = analizador;
    this.resultados = resultados;
    this.analisis = analisis;
    this.heteroevaluacion = heteroevaluacion;
    this.ventanas = ventanas;
    this.instruccion = instruccion;
    this.modelo = modelo;
    this.lector = lector;
    this.propiedades = propiedades;
    this.transaccion = transaccion;
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Revisión periódica de la cola; solo se programa si docentia.ia.worker.habilitado es true. */
  @Scheduled(
      fixedDelayString = "${docentia.ia.worker.intervalo-ms:5000}",
      initialDelayString = "${docentia.ia.worker.intervalo-ms:5000}")
  public void revisarCola() {
    while (procesarSiguiente()) {
      // sigue mientras haya solicitudes en cola
    }
  }

  /**
   * Procesa una solicitud de la cola, si hay.
   *
   * @return verdadero si procesó una (sin importar el resultado), falso si la cola estaba vacía
   */
  public boolean procesarSiguiente() {
    Optional<Long> tomada = tomarSiguiente();
    if (tomada.isEmpty()) {
      return false;
    }
    Long idSolicitud = tomada.get();
    try {
      Optional<Preparado> preparado = transaccion.execute(estado -> preparar(idSolicitud));
      if (preparado.isPresent()) {
        Optional<Generado> generado = generar(idSolicitud, preparado.get().peticion());
        if (generado.isPresent()) {
          transaccion.executeWithoutResult(
              estado -> guardarBorrador(idSolicitud, preparado.get(), generado.get()));
        } else {
          marcarError(idSolicitud);
        }
      }
    } catch (RuntimeException inesperada) {
      LOG.error("Falló el análisis de la solicitud {}", idSolicitud, inesperada);
      marcarError(idSolicitud);
    }
    return true;
  }

  private Optional<Long> tomarSiguiente() {
    return transaccion.execute(
        estado -> jdbcTemplate.queryForList(TOMAR_SIGUIENTE, Long.class).stream().findFirst());
  }

  private static final String TOMAR_SIGUIENTE =
      """
      update solicitudes_analisis
      set estado = 'EN_PROCESO', fecha_inicio = clock_timestamp(), intentos = intentos + 1
      where id_solicitud = (
          select id_solicitud from solicitudes_analisis
          where estado = 'EN_COLA'
          order by id_solicitud
          for update skip locked
          limit 1)
      returning id_solicitud
      """;

  private Optional<Preparado> preparar(Long idSolicitud) {
    SolicitudAnalisis solicitud = solicitudRepository.findById(idSolicitud).orElseThrow();
    Long idDocente = solicitud.getDocente().getIdUsuario();
    Long idPeriodo = solicitud.getPeriodo().getIdPeriodo();
    boolean formulario = solicitud.getFuente() == FuenteComentarios.FORMULARIO;
    if (formulario
        && !ventanas.haTerminado(solicitud.getPeriodo(), TipoEtapa.RECOLECCION_CALIFICACIONES)) {
      // El servicio ya lo impide al solicitar; esto cubre una ventana reprogramada después.
      solicitud.marcarInsuficiente(
          0, "la recolección del formulario sigue abierta", "VENTANA_ABIERTA");
      avisar(solicitud, TipoNotificacion.ANALISIS_INSUFICIENTE);
      return Optional.empty();
    }
    int encuestas =
        proveedores.para(solicitud.getFuente()).contarEncuestasCerradas(idDocente, idPeriodo);
    int minimo =
        formulario
            ? Math.max(propiedades.minimoEncuestas(), heteroevaluacion.minimoRespuestas())
            : propiedades.minimoEncuestas();
    if (encuestas < minimo) {
      solicitud.marcarInsuficiente(
          encuestas,
          "evidencia insuficiente (" + encuestas + " de " + minimo + " requeridas)",
          "POCAS_ENCUESTAS");
      avisar(solicitud, TipoNotificacion.ANALISIS_INSUFICIENTE);
      return Optional.empty();
    }
    PreparadorComentarios.Preparados preparados =
        preparador.preparar(solicitud, analisis.maximoComentarios());
    solicitud.registrarFiltrados(preparados.filtrados());
    if (preparados.leidos() == 0) {
      solicitud.marcarInsuficiente(
          encuestas, "no hay comentarios abiertos para analizar", "SIN_COMENTARIOS");
      avisar(solicitud, TipoNotificacion.ANALISIS_INSUFICIENTE);
      return Optional.empty();
    }
    if (preparados.enviados().isEmpty()) {
      solicitud.marcarInsuficiente(
          encuestas,
          "evidencia insuficiente: el filtro excluyó todos los comentarios",
          "TODOS_EXCLUIDOS");
      avisar(solicitud, TipoNotificacion.ANALISIS_INSUFICIENTE);
      return Optional.empty();
    }
    return Optional.of(
        new Preparado(
            new PeticionAnalisis(instruccion.texto(), preparados.enviados()), preparados));
  }

  /**
   * Llama al modelo y valida su respuesta; si el JSON no sirve lo intenta una vez más.
   *
   * @return los hallazgos con el modelo que los generó, o vacío si no se logró
   */
  private Optional<Generado> generar(Long idSolicitud, PeticionAnalisis peticion) {
    for (int intento = 1; intento <= 2; intento++) {
      try {
        RespuestaModelo respuesta = modelo.analizar(peticion);
        return Optional.of(new Generado(lector.leer(respuesta.contenido()), respuesta.modelo()));
      } catch (RespuestaInvalidaException invalida) {
        LOG.warn(
            "Respuesta inválida del modelo para la solicitud {} (intento {}): {}",
            idSolicitud,
            intento,
            invalida.getMessage());
      } catch (ModeloIAException fallo) {
        LOG.warn("El modelo falló para la solicitud {}: {}", idSolicitud, fallo.getMessage());
        break;
      }
    }
    return Optional.empty();
  }

  private void guardarBorrador(Long idSolicitud, Preparado preparado, Generado generado) {
    PeticionAnalisis peticion = preparado.peticion();
    SolicitudAnalisis solicitud = solicitudRepository.findById(idSolicitud).orElseThrow();
    // Solo cuentan los comentarios que de verdad se enviaron: no filtrados, del docente y periodo.
    VerificadorCitas verificador = new VerificadorCitas(peticion.comentarios());
    List<Hallazgo> hallazgos = new ArrayList<>();
    int orden = 1;
    for (HallazgoPropuesto propuesto : generado.hallazgos()) {
      Hallazgo hallazgo = new Hallazgo(orden++, propuesto.tipo(), propuesto.texto());
      propuesto
          .citas()
          .forEach(
              cita ->
                  hallazgo.agregarCita(
                      new CitaHallazgo(
                          cita.comentarioId(),
                          cita.fragmento(),
                          verificador.esVerificable(cita.comentarioId(), cita.fragmento()))));
      hallazgos.add(hallazgo);
    }
    int encuestas =
        proveedores
            .para(solicitud.getFuente())
            .contarEncuestasCerradas(
                solicitud.getDocente().getIdUsuario(), solicitud.getPeriodo().getIdPeriodo());
    solicitud.completar(
        hallazgos,
        encuestas,
        peticion.comentarios().size(),
        generado.modelo(),
        instruccion.version());
    guardarResultado(solicitud, preparado.preparados(), generado.modelo());
    avisar(solicitud, TipoNotificacion.ANALISIS_TERMINADO);
  }

  /**
   * Resultado estructurado y copia anonimizada. Los conteos, temas y tendencias los calcula siempre
   * el modelo local, que es determinista y verificable; si los hallazgos vienen de un modelo de
   * lenguaje el resultado se rotula LLM y lo advierte.
   */
  private void guardarResultado(
      SolicitudAnalisis solicitud,
      PreparadorComentarios.Preparados preparados,
      String modeloUsado) {
    Long idDocente = solicitud.getDocente().getIdUsuario();
    Long idPeriodo = solicitud.getPeriodo().getIdPeriodo();
    Salida salida =
        analizador.analizar(
            preparados.preparacion().utiles(),
            resultados.anterior(idDocente, idPeriodo).orElse(null));
    ResultadoAnalisis resultado = pipeline.completar(salida.resultado(), preparados.preparacion());
    List<Advertencia> advertencias = new ArrayList<>(resultado.advertencias());
    if (preparados.recortado()) {
      advertencias.add(
          new Advertencia(
              "LIMITE_COMENTARIOS",
              analisis.maximoComentarios(),
              "Se analizaron los primeros "
                  + analisis.maximoComentarios()
                  + " comentarios útiles en el orden de su huella."));
    }
    boolean llm = !"REGLAS".equals(modelo.tipo());
    if (llm) {
      advertencias.add(
          new Advertencia(
              "CONTEO_POR_REGLAS",
              0,
              "Los hallazgos los propuso un modelo de lenguaje; los conteos, temas y tendencias"
                  + " los calcula el modelo local por reglas."));
    }
    resultado =
        new ResultadoAnalisis(
            llm ? "LLM" : "REGLAS",
            llm ? modeloUsado : resultado.versionModelo(),
            llm ? instruccion.version() : resultado.versionLexicoOPrompt(),
            resultado.nComentarios(),
            resultado.nExcluidos(),
            resultado.sentimientoGlobal(),
            resultado.temas(),
            resultado.aspectos(),
            resultado.tendencias(),
            advertencias,
            resultado.criterios(),
            resultado.parametros());
    Map<Long, Etiquetado> etiquetados =
        salida.etiquetados().stream()
            .collect(Collectors.toMap(Etiquetado::id, Function.identity(), (a, b) -> a));
    List<ComentarioGuardado> copia = new ArrayList<>();
    int indice = 1;
    for (Comentario comentario : preparados.preparacion().utiles()) {
      Etiquetado etiquetado = etiquetados.get(comentario.id());
      copia.add(
          new ComentarioGuardado(
              indice++,
              comentario.id(),
              PipelineAnalisis.hash(comentario.texto()),
              comentario.texto(),
              etiquetado == null ? "NEUTRO" : etiquetado.sentimiento(),
              etiquetado == null ? List.of() : etiquetado.etiquetas()));
    }
    resultados.guardar(
        solicitud.getIdSolicitud(), idDocente, idPeriodo, solicitud.getFuente(), resultado, copia);
  }

  /** Avisa a quien pidió el análisis, que por construcción está en la facultad del docente. */
  private void avisar(SolicitudAnalisis solicitud, TipoNotificacion tipo) {
    eventos.publishEvent(
        EventoNotificacion.aUsuario(
            tipo,
            solicitud.getSolicitante().getIdUsuario(),
            "/analisis?docente="
                + solicitud.getDocente().getIdUsuario()
                + "&periodo="
                + solicitud.getPeriodo().getIdPeriodo(),
            "SOLICITUD_ANALISIS",
            solicitud.getIdSolicitud(),
            "solicitud:" + solicitud.getIdSolicitud()));
  }

  private void marcarError(Long idSolicitud) {
    transaccion.executeWithoutResult(
        estado ->
            solicitudRepository
                .findById(idSolicitud)
                .filter(solicitud -> solicitud.getEstado() == EstadoSolicitudAnalisis.EN_PROCESO)
                .ifPresent(
                    solicitud -> {
                      solicitud.marcarError(MENSAJE_ERROR);
                      avisar(solicitud, TipoNotificacion.ANALISIS_ERROR);
                    }));
  }

  private record Generado(List<HallazgoPropuesto> hallazgos, String modelo) {}

  private record Preparado(
      PeticionAnalisis peticion, PreparadorComentarios.Preparados preparados) {}
}
