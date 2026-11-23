package com.uniautonoma.docentia.service.analisis;

import com.uniautonoma.docentia.config.AhorroIaProperties;
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
import com.uniautonoma.docentia.service.analisis.LectorRespuestaAnalisis.RespuestaCortadaException;
import com.uniautonoma.docentia.service.analisis.LectorRespuestaAnalisis.RespuestaInvalidaException;
import com.uniautonoma.docentia.service.analisis.ModeloIA.ModeloIAException;
import com.uniautonoma.docentia.service.analisis.ModeloIA.PeticionAnalisis;
import com.uniautonoma.docentia.service.analisis.ModeloIA.RespuestaModelo;
import com.uniautonoma.docentia.service.analisis.ModeloIA.Uso;
import com.uniautonoma.docentia.service.analisis.ResultadosAnalisisRepositorio.Anterior;
import com.uniautonoma.docentia.service.analisis.ResultadosAnalisisRepositorio.ComentarioGuardado;
import com.uniautonoma.docentia.service.analisis.UsoIaRepositorio.EnCache;
import com.uniautonoma.docentia.service.analisis.UsoIaRepositorio.Registro;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Comentario;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Etiquetado;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorComentarios.Salida;
import com.uniautonoma.docentia.service.analisis.local.AnalizadorLocal;
import com.uniautonoma.docentia.service.analisis.local.ModeloIALocal;
import com.uniautonoma.docentia.service.analisis.local.MuestreoParaModelo;
import com.uniautonoma.docentia.service.analisis.local.MuestreoParaModelo.Muestra;
import com.uniautonoma.docentia.service.analisis.local.PipelineAnalisis;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Advertencia;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.LimitesAplicados;
import com.uniautonoma.docentia.service.analisis.local.ResultadoAnalisis.Tema;
import com.uniautonoma.docentia.service.notificacion.EventoNotificacion;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

  /** Motivos guardados en la solicitud cuando el análisis sale del modelo local de respaldo. */
  static final String PROVEEDOR_CONFIGURACION = "PROVEEDOR_CONFIGURACION";

  static final String PROVEEDOR_NO_DISPONIBLE = "PROVEEDOR_NO_DISPONIBLE";
  static final String RESPUESTA_INVALIDA = "RESPUESTA_INVALIDA";
  static final String RESPUESTA_CORTADA = "RESPUESTA_CORTADA";
  static final String LLM_NO_DISPONIBLE = "LLM_NO_DISPONIBLE";
  static final String MODELO_LOCAL_FALLO = "MODELO_LOCAL_FALLO";

  /** Lo que lee el usuario en el resultado: qué pasó con el externo y que respondió el local. */
  static final Map<String, String> MENSAJES_RESPALDO =
      Map.of(
          PROVEEDOR_CONFIGURACION,
          "El proveedor externo rechazó la configuración (modelo, clave o parámetros), así que"
              + " este análisis lo hizo el modelo local por reglas. La administración ya fue"
              + " avisada.",
          PROVEEDOR_NO_DISPONIBLE,
          "El proveedor externo no respondió después de varios intentos, así que este análisis lo"
              + " hizo el modelo local por reglas.",
          RESPUESTA_INVALIDA,
          "La respuesta del proveedor externo no tuvo el formato esperado, así que este análisis"
              + " lo hizo el modelo local por reglas.",
          RESPUESTA_CORTADA,
          "La respuesta del proveedor externo llegó incompleta, así que este análisis lo hizo el"
              + " modelo local por reglas.",
          LLM_NO_DISPONIBLE,
          "No hay un modelo de lenguaje configurado en este ambiente, así que este análisis lo"
              + " hizo el modelo local por reglas. La administración ya fue avisada.");

  /** Reintentos ante un fallo transitorio: tres llamadas en total. */
  static final int REINTENTOS_TRANSITORIOS = 2;

  /** Tope de una espera y de todas juntas, para no tener el procesador detenido mucho tiempo. */
  static final long ESPERA_UNICA_MAXIMA_MS = 30_000;

  static final long ESPERA_MAXIMA_MS = 45_000;

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
  private final AhorroIaProperties ahorro;
  private final UsoIaRepositorio uso;
  private final ModeloIA modeloLocal;
  private final ModelosDisponibles modelos;
  private final InstruccionAnalisis instruccion;
  private final ModeloIA modelo;
  private final LectorRespuestaAnalisis lector;
  private final IaProperties propiedades;
  private final TransactionTemplate transaccion;
  private final JdbcTemplate jdbcTemplate;
  private final long esperaBaseMs;

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
      AhorroIaProperties ahorro,
      UsoIaRepositorio uso,
      ModelosDisponibles modelos,
      InstruccionAnalisis instruccion,
      ModeloIA modelo,
      LectorRespuestaAnalisis lector,
      IaProperties propiedades,
      TransactionTemplate transaccion,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher eventos,
      @Value("${docentia.ia.reintento-base-ms:2000}") long esperaBaseMs) {
    this.esperaBaseMs = esperaBaseMs;
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
    this.ahorro = ahorro;
    this.uso = uso;
    this.modeloLocal = new ModeloIALocal(analizador);
    this.modelos = modelos;
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
        Intento intento = generar(idSolicitud, preparado.get());
        Generado generado = intento.generado();
        if (generado != null && generado.rechazado()) {
          marcarSinPresupuesto(idSolicitud);
        } else if (generado != null) {
          transaccion.executeWithoutResult(
              estado -> guardarBorrador(idSolicitud, preparado.get(), generado));
        } else if (!"REGLAS".equals(preparado.get().modelo().tipo())) {
          usarModeloLocal(idSolicitud, preparado.get(), intento.fallo());
        } else {
          marcarError(idSolicitud);
        }
      }
    } catch (RuntimeException inesperada) {
      LOG.error("Falló el análisis de la solicitud {}: {}", idSolicitud, describir(inesperada));
      marcarError(idSolicitud);
    }
    return true;
  }

  /**
   * El externo falló de forma definitiva: el análisis lo hace el modelo local, que no usa red, y el
   * resultado queda rotulado con el motivo. Solo si el local también falla queda en ERROR.
   */
  private void usarModeloLocal(Long idSolicitud, Preparado externo, String motivo) {
    LOG.warn("Solicitud {}: se usa el modelo local por {}", idSolicitud, motivo);
    Preparado local = respaldoLocal(externo.preparados(), externo.idPeriodo(), motivo);
    Generado generado = generar(idSolicitud, local).generado();
    if (generado == null) {
      marcarError(idSolicitud);
      return;
    }
    transaccion.executeWithoutResult(estado -> guardarBorrador(idSolicitud, local, generado));
  }

  /** El modelo local lee todos los comentarios preparados, no la muestra del externo. */
  private Preparado respaldoLocal(
      PreparadorComentarios.Preparados preparados, Long idPeriodo, String motivo) {
    return new Preparado(
        new PeticionAnalisis(instruccion.texto(), preparados.enviados()),
        preparados,
        modeloLocal,
        null,
        idPeriodo,
        motivo);
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
    ProveedorComentarios proveedor = proveedores.para(solicitud.getFuente());
    int encuestas = proveedor.contarEncuestasCerradas(idDocente, idPeriodo);
    int comentarios = proveedor.contarComentarios(idDocente, idPeriodo);
    int minimo = propiedades.minimoComentarios();
    if (comentarios == 0) {
      solicitud.marcarInsuficiente(
          encuestas, "no hay comentarios abiertos para analizar", "SIN_COMENTARIOS");
      avisar(solicitud, TipoNotificacion.ANALISIS_INSUFICIENTE);
      return Optional.empty();
    }
    if (comentarios < minimo) {
      solicitud.marcarInsuficiente(
          comentarios,
          "evidencia insuficiente (" + comentarios + " de " + minimo + " comentarios requeridos)",
          "POCOS_COMENTARIOS");
      avisar(solicitud, TipoNotificacion.ANALISIS_INSUFICIENTE);
      return Optional.empty();
    }
    PreparadorComentarios.Preparados preparados =
        preparador.preparar(solicitud, analisis.maximoComentarios());
    solicitud.registrarFiltrados(preparados.filtrados());
    solicitud.registrarComentariosLeidos(comentarios);
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
    ModeloIA elegido =
        solicitud.esSoloLocal()
            ? modeloLocal
            : solicitud.pideLlm() ? modelos.externo().orElse(null) : modelo;
    if (elegido == null) {
      // Pidieron el modelo de lenguaje y ya no está configurado: responde el local, rotulado, y se
      // avisa para que la configuración se corrija.
      LOG.error(
          "Solicitud {}: pidieron el modelo de lenguaje y no está configurado; se usa el local",
          idSolicitud);
      avisarConfiguracion(idSolicitud);
      return Optional.of(respaldoLocal(preparados, idPeriodo, LLM_NO_DISPONIBLE));
    }
    if ("REGLAS".equals(elegido.tipo())) {
      return Optional.of(
          new Preparado(
              new PeticionAnalisis(instruccion.texto(), preparados.enviados()),
              preparados,
              elegido,
              null,
              idPeriodo));
    }
    // Modelo de lenguaje: el local ya contó todo; al externo va una muestra compacta.
    Salida local = analizador.analizar(preparados.preparacion().utiles(), null);
    List<String> temas =
        local.resultado().temas().stream()
            .limit(10)
            .map(ProcesadorSolicitudesAnalisis::compacto)
            .toList();
    Muestra muestra =
        new MuestreoParaModelo(
                ahorro.maxComentariosLlm(),
                ahorro.maxCaracteresComentario(),
                ahorro.maxTokensEntrada())
            .elegir(
                preparados.preparacion().utiles(), local.etiquetados(), instruccion.texto(), temas);
    return Optional.of(
        new Preparado(
            new PeticionAnalisis(instruccion.texto(), muestra.enviados(), temas),
            preparados,
            elegido,
            muestra,
            idPeriodo));
  }

  /** D05+4 o D07-3: descriptor, signo de la polaridad y frecuencia. */
  private static String compacto(Tema tema) {
    String signo =
        switch (tema.polaridad()) {
          case "POSITIVO" -> "+";
          case "NEGATIVO" -> "-";
          default -> "~";
        };
    return tema.descriptor() + signo + tema.frecuencia();
  }

  /**
   * Llama al modelo y valida su respuesta. Con un modelo de lenguaje, antes de llamar busca la
   * misma entrada en la caché y revisa el presupuesto, y trata cada clase de fallo distinto:
   *
   * <ul>
   *   <li>configuración (400, 401, 403, 404): no se reintenta y se avisa;
   *   <li>transitorio (429, 5xx, tiempo, red): hasta {@value #REINTENTOS_TRANSITORIOS} reintentos
   *       con espera creciente, respetando el {@code Retry-After} y un tope total;
   *   <li>respuesta cortada: un intento más con la mitad de los comentarios;
   *   <li>JSON mal formado: un intento más, igual.
   * </ul>
   *
   * @return los hallazgos con el modelo que los generó, o el motivo por el que no se lograron
   */
  private Intento generar(Long idSolicitud, Preparado preparado) {
    ModeloIA elegido = preparado.modelo();
    PeticionAnalisis peticion = preparado.peticion();
    if ("REGLAS".equals(elegido.tipo())) {
      try {
        RespuestaModelo respuesta = elegido.analizar(peticion);
        return Intento.logrado(
            new Generado(
                lector.leer(respuesta.contenido()), respuesta.modelo(), null, false, false));
      } catch (RuntimeException fallo) {
        LOG.error("El modelo local falló para la solicitud {}: {}", idSolicitud, describir(fallo));
        return Intento.fallido(MODELO_LOCAL_FALLO);
      }
    }
    Muestra muestra = preparado.muestra();
    String huella = huella(peticion, elegido);
    String modeloConfigurado = nombreModelo(elegido);
    Optional<EnCache> enCache = uso.buscar(huella);
    if (enCache.isPresent()) {
      registrar(
          preparado,
          huella,
          enCache.get().tokensEntrada(),
          enCache.get().tokensSalida(),
          0,
          false,
          "ACIERTO_CACHE",
          0);
      try {
        return Intento.logrado(
            new Generado(
                lector.leer(enCache.get().contenido()),
                enCache.get().modelo(),
                lector.resumen(enCache.get().contenido()),
                true,
                false));
      } catch (RespuestaInvalidaException ilegible) {
        LOG.warn("La caché de la solicitud {} no se pudo leer; se llama al proveedor", idSolicitud);
      }
    }
    long estimado = (long) muestra.tokensEstimados() + ahorro.maxTokensSalida();
    if (sinPresupuesto(preparado.idPeriodo(), estimado)) {
      registrar(preparado, huella, 0, 0, 0, true, "RECHAZADO_PRESUPUESTO", 0);
      return Intento.logrado(new Generado(List.of(), modeloConfigurado, null, false, true));
    }
    PeticionAnalisis actual = peticion;
    String huellaActual = huella;
    int transitorios = 0;
    long esperadoMs = 0;
    boolean puedeRepetirInvalida = true;
    boolean puedePartirCortada = true;
    for (int intento = 1; ; intento++) {
      long inicio = System.nanoTime();
      try {
        RespuestaModelo respuesta = elegido.analizar(actual);
        long duracion = (System.nanoTime() - inicio) / 1_000_000;
        Uso gasto =
            respuesta.uso() != null
                ? respuesta.uso()
                : new Uso(
                    EstimadorTokens.estimar(actual.instruccion())
                        + actual.comentarios().stream()
                            .mapToInt(c -> EstimadorTokens.estimar(c.texto()))
                            .sum(),
                    EstimadorTokens.estimar(respuesta.contenido()),
                    0,
                    true);
        registrar(
            preparado,
            huellaActual,
            gasto.entrada(),
            gasto.salida(),
            gasto.cache(),
            gasto.estimado(),
            "LLAMADA",
            duracion);
        List<HallazgoPropuesto> hallazgos = lector.leer(respuesta.contenido());
        uso.guardar(
            huellaActual,
            respuesta.modelo(),
            instruccion.version(),
            respuesta.contenido(),
            gasto.entrada(),
            gasto.salida());
        return Intento.logrado(
            new Generado(
                hallazgos,
                respuesta.modelo(),
                lector.resumen(respuesta.contenido()),
                false,
                false,
                actual == peticion ? null : actual));
      } catch (RespuestaCortadaException cortada) {
        LOG.warn(
            "Respuesta incompleta del modelo para la solicitud {} (intento {})",
            idSolicitud,
            intento);
        if (!puedePartirCortada || actual.comentarios().size() < 2 || !alcanza(preparado)) {
          return Intento.fallido(RESPUESTA_CORTADA);
        }
        puedePartirCortada = false;
        actual = mitad(actual);
        huellaActual = huella(actual, elegido);
      } catch (RespuestaInvalidaException invalida) {
        LOG.warn(
            "Respuesta inválida del modelo para la solicitud {} (intento {}): {}",
            idSolicitud,
            intento,
            invalida.getMessage());
        if (!puedeRepetirInvalida || !alcanza(preparado)) {
          return Intento.fallido(RESPUESTA_INVALIDA);
        }
        puedeRepetirInvalida = false;
      } catch (ModeloIAException fallo) {
        long duracion = (System.nanoTime() - inicio) / 1_000_000;
        Uso gasto = fallo.uso();
        registrar(
            preparado,
            huellaActual,
            gasto == null ? 0 : gasto.entrada(),
            gasto == null ? 0 : gasto.salida(),
            0,
            gasto != null && gasto.estimado(),
            "ERROR",
            duracion);
        LOG.warn(
            "El modelo falló para la solicitud {} (intento {}, {}): {}",
            idSolicitud,
            intento,
            fallo.clase(),
            fallo.getMessage());
        switch (fallo.clase()) {
          case CONFIGURACION -> {
            LOG.error(
                "El proveedor externo rechazó la configuración (solicitud {}): {}. Revisa"
                    + " DOCENTIA_IA_URL_BASE, DOCENTIA_IA_MODELO, DOCENTIA_IA_CLAVE y"
                    + " DOCENTIA_IA_ENVIAR_TEMPERATURA.",
                idSolicitud,
                fallo.getMessage());
            avisarConfiguracion(idSolicitud);
            return Intento.fallido(PROVEEDOR_CONFIGURACION);
          }
          case CORTADA -> {
            if (!puedePartirCortada || actual.comentarios().size() < 2 || !alcanza(preparado)) {
              return Intento.fallido(RESPUESTA_CORTADA);
            }
            puedePartirCortada = false;
            actual = mitad(actual);
            huellaActual = huella(actual, elegido);
          }
          case INVALIDA -> {
            if (!puedeRepetirInvalida || !alcanza(preparado)) {
              return Intento.fallido(RESPUESTA_INVALIDA);
            }
            puedeRepetirInvalida = false;
          }
          case TRANSITORIO -> {
            long espera = espera(transitorios, fallo.esperaSugerida());
            if (transitorios >= REINTENTOS_TRANSITORIOS || esperadoMs + espera > ESPERA_MAXIMA_MS) {
              return Intento.fallido(PROVEEDOR_NO_DISPONIBLE);
            }
            transitorios++;
            esperadoMs += espera;
            esperar(espera);
          }
        }
      } catch (RuntimeException inesperada) {
        LOG.error(
            "Falló la llamada al proveedor externo (solicitud {}): {}",
            idSolicitud,
            describir(inesperada));
        return Intento.fallido(PROVEEDOR_NO_DISPONIBLE);
      }
    }
  }

  /** Antes de repetir una llamada que sí gasta tokens, el presupuesto debe alcanzar otra vez. */
  private boolean alcanza(Preparado preparado) {
    long estimado = (long) preparado.muestra().tokensEstimados() + ahorro.maxTokensSalida();
    return !sinPresupuesto(preparado.idPeriodo(), estimado);
  }

  /** La primera mitad de los comentarios: una respuesta más corta que quepa en el tope. */
  private static PeticionAnalisis mitad(PeticionAnalisis peticion) {
    List<ModeloIA.ComentarioAnonimo> comentarios = peticion.comentarios();
    return new PeticionAnalisis(
        peticion.instruccion(),
        List.copyOf(comentarios.subList(0, (comentarios.size() + 1) / 2)),
        peticion.temas());
  }

  /**
   * Espera antes del reintento {@code n} (desde 0): base, base×4, base×16…, o lo que pidió el
   * proveedor si es más, con un tope por espera.
   */
  long espera(int n, Duration sugerida) {
    long creciente = esperaBaseMs * (1L << (2 * Math.min(n, 10)));
    long pedida = sugerida == null ? 0 : sugerida.toMillis();
    return Math.min(Math.max(creciente, pedida), ESPERA_UNICA_MAXIMA_MS);
  }

  private void esperar(long milisegundos) {
    if (milisegundos <= 0) {
      return;
    }
    try {
      Thread.sleep(milisegundos);
    } catch (InterruptedException interrumpido) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * El proveedor rechazó la configuración: el análisis sale con el modelo local, pero la
   * administración y la vicerrectoría lo saben (un aviso por día) para que no quede escondido.
   */
  private void avisarConfiguracion(Long idSolicitud) {
    String clave = "proveedor-ia:" + ventanas.hoy();
    // Los avisos salen al confirmar una transacción; fuera de una se perderían.
    transaccion.executeWithoutResult(
        estado -> {
          eventos.publishEvent(
              EventoNotificacion.aAdministradores(
                  TipoNotificacion.PROVEEDOR_IA_RECHAZO,
                  null,
                  "SOLICITUD_ANALISIS",
                  idSolicitud,
                  clave));
          eventos.publishEvent(
              EventoNotificacion.aVicerrectores(
                  TipoNotificacion.PROVEEDOR_IA_RECHAZO,
                  null,
                  "SOLICITUD_ANALISIS",
                  idSolicitud,
                  clave));
        });
  }

  /**
   * Huella de la llamada: proveedor, modelo, versión de la instrucción, topes y la entrada exacta
   * (ya anonimizada y ordenada por huella). Es un SHA-256: no permite reconstruir los comentarios.
   */
  private String huella(PeticionAnalisis peticion, ModeloIA elegido) {
    return huella(
        nombreModelo(elegido),
        instruccion.version(),
        ahorro.maxComentariosLlm()
            + "|"
            + ahorro.maxCaracteresComentario()
            + "|"
            + ahorro.maxTokensEntrada()
            + "|"
            + ahorro.maxTokensSalida(),
        peticion);
  }

  static String huella(String modelo, String version, String topes, PeticionAnalisis peticion) {
    StringBuilder base =
        new StringBuilder()
            .append(modelo)
            .append('\n')
            .append(version)
            .append('\n')
            .append(topes)
            .append('\n')
            .append(String.join(",", peticion.temas()))
            .append('\n');
    peticion
        .comentarios()
        .forEach(c -> base.append(c.id()).append('\t').append(c.texto()).append('\n'));
    return PipelineAnalisis.hash(base.toString());
  }

  private String nombreModelo(ModeloIA elegido) {
    String nombre = propiedades.modelo();
    return proveedor(elegido) + ":" + (nombre == null || nombre.isBlank() ? "-" : nombre);
  }

  /** Con el local por defecto, el externo pedido por solicitud es chat-completions. */
  private String proveedor(ModeloIA elegido) {
    return modelos.esExternoAparte(elegido) ? "chat-completions" : propiedades.proveedor();
  }

  private boolean sinPresupuesto(Long idPeriodo, long estimado) {
    if (ahorro.presupuestoTokensDia() > 0
        && uso.tokensDelDia(ventanas.hoy()) + estimado > ahorro.presupuestoTokensDia()) {
      return true;
    }
    return ahorro.presupuestoTokensPeriodo() > 0
        && uso.tokensDelPeriodo(idPeriodo) + estimado > ahorro.presupuestoTokensPeriodo();
  }

  private void registrar(
      Preparado preparado,
      String huella,
      int entrada,
      int salida,
      int cache,
      boolean estimado,
      String resultado,
      long duracionMs) {
    Muestra muestra = preparado.muestra();
    uso.registrar(
        new Registro(
            preparado.idPeriodo(),
            proveedor(preparado.modelo()),
            nombreModelo(preparado.modelo()),
            instruccion.version(),
            huella,
            muestra.disponibles(),
            muestra.enviados().size(),
            entrada,
            salida,
            cache,
            muestra.tokensSinTopes(),
            estimado,
            resultado,
            duracionMs));
  }

  private void guardarBorrador(Long idSolicitud, Preparado preparado, Generado generado) {
    PeticionAnalisis peticion =
        generado.enviada() != null ? generado.enviada() : preparado.peticion();
    SolicitudAnalisis solicitud = solicitudRepository.findById(idSolicitud).orElseThrow();
    // Solo cuentan los comentarios que de verdad se enviaron: no filtrados, del docente y periodo.
    VerificadorCitas verificador = new VerificadorCitas(peticion.comentarios());
    Map<Long, String> enviados = new HashMap<>();
    peticion.comentarios().forEach(c -> enviados.put(c.id(), c.texto()));
    List<Hallazgo> hallazgos = new ArrayList<>();
    int orden = 1;
    for (HallazgoPropuesto propuesto : generado.hallazgos()) {
      Hallazgo hallazgo = new Hallazgo(orden++, propuesto.tipo(), propuesto.texto());
      propuesto
          .citas()
          .forEach(
              cita -> {
                if (cita.fragmento() == null) {
                  // Cita por id: el texto lo pone el sistema; un id que no se envió no vale.
                  String texto =
                      cita.comentarioId() == null ? null : enviados.get(cita.comentarioId());
                  hallazgo.agregarCita(
                      new CitaHallazgo(
                          cita.comentarioId(),
                          texto == null ? "(comentario inexistente)" : texto,
                          texto != null));
                } else {
                  hallazgo.agregarCita(
                      new CitaHallazgo(
                          cita.comentarioId(),
                          cita.fragmento(),
                          verificador.esVerificable(cita.comentarioId(), cita.fragmento())));
                }
              });
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
    if (preparado.respaldo() != null) {
      solicitud.registrarRespaldoLocal(
          preparado.respaldo(), MENSAJES_RESPALDO.get(preparado.respaldo()));
    }
    guardarResultado(solicitud, preparado, generado);
    avisar(solicitud, TipoNotificacion.ANALISIS_TERMINADO);
  }

  /**
   * Resultado estructurado y copia anonimizada. Los conteos, temas y tendencias los calcula siempre
   * el modelo local, que es determinista y verificable; si los hallazgos vienen de un modelo de
   * lenguaje el resultado se rotula LLM y lo advierte.
   */
  private void guardarResultado(
      SolicitudAnalisis solicitud, Preparado preparado, Generado generado) {
    PreparadorComentarios.Preparados preparados = preparado.preparados();
    String modeloUsado = generado.modelo();
    Long idDocente = solicitud.getDocente().getIdUsuario();
    Long idPeriodo = solicitud.getPeriodo().getIdPeriodo();
    List<Anterior> anteriores =
        resultados.anteriores(idDocente, idPeriodo, ahorro.periodosReferenciaAcotados());
    Salida salida = analizador.analizar(preparados.preparacion().utiles(), combinar(anteriores));
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
    boolean llm = !"REGLAS".equals(preparado.modelo().tipo());
    if (preparado.respaldo() != null) {
      // Cuenta 1 por análisis: el resumen de estadísticas suma cuántos salieron del respaldo.
      advertencias.add(
          new Advertencia("RESPALDO_LOCAL", 1, MENSAJES_RESPALDO.get(preparado.respaldo())));
    }
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
    int analizados = preparados.preparacion().utiles().size();
    Muestra muestra = preparado.muestra();
    LimitesAplicados limites =
        new LimitesAplicados(
            analizados,
            muestra == null ? analizados : muestra.enviados().size(),
            muestra != null && muestra.muestreado(),
            muestra == null ? 0 : muestra.recortados(),
            muestra == null ? 0 : muestra.sinContenido(),
            ahorro.maxComentariosLlm(),
            ahorro.maxCaracteresComentario(),
            anteriores.stream().map(Anterior::idPeriodo).toList());
    resultado =
        resultado.conCorrida(
            limites, generado.desdeCache(), LocalDateTime.now().toString(), generado.resumen());
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

  static final String MENSAJE_PRESUPUESTO =
      "Se agotó el presupuesto de tokens del proveedor externo. Puedes pedir el análisis con el"
          + " modelo local por reglas o esperar a que se renueve el presupuesto.";

  private void marcarSinPresupuesto(Long idSolicitud) {
    transaccion.executeWithoutResult(
        estado ->
            solicitudRepository
                .findById(idSolicitud)
                .filter(solicitud -> solicitud.getEstado() == EstadoSolicitudAnalisis.EN_PROCESO)
                .ifPresent(
                    solicitud -> {
                      solicitud.marcarError(MENSAJE_PRESUPUESTO, "PRESUPUESTO_AGOTADO");
                      avisar(solicitud, TipoNotificacion.ANALISIS_ERROR);
                    }));
  }

  /**
   * Une los agregados de los periodos de referencia en uno solo: porcentaje promedio de cada tema y
   * comentarios sumados. Con un periodo queda igual.
   */
  private static ResultadoAnalisis combinar(List<Anterior> anteriores) {
    if (anteriores.isEmpty()) {
      return null;
    }
    if (anteriores.size() == 1) {
      return anteriores.get(0).resultado();
    }
    Map<String, double[]> porcentajes = new LinkedHashMap<>();
    Map<String, Tema> ejemplo = new LinkedHashMap<>();
    int comentarios = 0;
    for (Anterior anterior : anteriores) {
      comentarios += anterior.resultado().nComentarios();
      for (Tema tema : anterior.resultado().temas()) {
        String clave = tema.descriptor() + "|" + tema.polaridad();
        porcentajes.computeIfAbsent(clave, k -> new double[1])[0] += tema.porcentaje();
        ejemplo.putIfAbsent(clave, tema);
      }
    }
    int n = anteriores.size();
    List<Tema> temas = new ArrayList<>();
    porcentajes.forEach(
        (clave, suma) -> {
          Tema t = ejemplo.get(clave);
          temas.add(
              new Tema(
                  t.criterio(),
                  t.descriptor(),
                  t.nombre(),
                  t.polaridad(),
                  t.frecuencia(),
                  Math.round(suma[0] / n * 100) / 100.0,
                  t.intensidad(),
                  t.relevancia(),
                  t.recurrente(),
                  List.of()));
        });
    ResultadoAnalisis base = anteriores.get(0).resultado();
    return new ResultadoAnalisis(
        base.tipoModelo(),
        base.versionModelo(),
        base.versionLexicoOPrompt(),
        comentarios,
        Map.of(),
        base.sentimientoGlobal(),
        temas,
        base.aspectos(),
        List.of(),
        List.of(),
        base.criterios(),
        base.parametros());
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

  /**
   * @param resumen resumen narrativo del modelo de lenguaje, si lo dio
   * @param rechazado no se llamó porque no alcanzaba el presupuesto
   * @param enviada lo que de verdad recibió el modelo (la mitad, si se partió una respuesta
   *     cortada); nula si fue la petición preparada
   */
  private record Generado(
      List<HallazgoPropuesto> hallazgos,
      String modelo,
      String resumen,
      boolean desdeCache,
      boolean rechazado,
      PeticionAnalisis enviada) {

    Generado(
        List<HallazgoPropuesto> hallazgos,
        String modelo,
        String resumen,
        boolean desdeCache,
        boolean rechazado) {
      this(hallazgos, modelo, resumen, desdeCache, rechazado, null);
    }
  }

  /** Lo que dejó la generación: los hallazgos, o el motivo por el que no se lograron. */
  private record Intento(Generado generado, String fallo) {

    static Intento logrado(Generado generado) {
      return new Intento(generado, null);
    }

    static Intento fallido(String motivo) {
      return new Intento(null, motivo);
    }
  }

  /**
   * @param muestra lo que va al modelo de lenguaje; nula con el modelo local, que lee todo
   * @param respaldo motivo por el que se usa el modelo local en lugar del externo pedido; nulo si
   *     es el modelo que se pidió
   */
  private record Preparado(
      PeticionAnalisis peticion,
      PreparadorComentarios.Preparados preparados,
      ModeloIA modelo,
      Muestra muestra,
      Long idPeriodo,
      String respaldo) {

    Preparado(
        PeticionAnalisis peticion,
        PreparadorComentarios.Preparados preparados,
        ModeloIA modelo,
        Muestra muestra,
        Long idPeriodo) {
      this(peticion, preparados, modelo, muestra, idPeriodo, null);
    }
  }

  /**
   * Tipos de la cadena de causas y el punto donde ocurrió, sin mensajes: el mensaje de un error de
   * base de datos o de un proveedor puede repetir la fila o la petición, con texto de comentarios.
   */
  static String describir(Throwable error) {
    StringBuilder texto = new StringBuilder();
    Throwable actual = error;
    for (int nivel = 0; actual != null && nivel < 5; nivel++) {
      if (nivel > 0) {
        texto.append(" <- ");
      }
      texto.append(actual.getClass().getName());
      StackTraceElement[] pila = actual.getStackTrace();
      if (pila.length > 0) {
        texto.append(" en ").append(pila[0]);
      }
      actual = actual.getCause() == actual ? null : actual.getCause();
    }
    return texto.toString();
  }
}
