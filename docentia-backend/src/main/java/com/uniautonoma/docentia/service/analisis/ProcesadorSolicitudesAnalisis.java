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
        Optional<Generado> generado = generar(idSolicitud, preparado.get());
        if (generado.isPresent() && generado.get().rechazado()) {
          marcarSinPresupuesto(idSolicitud);
        } else if (generado.isPresent()) {
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
    ModeloIA elegido =
        solicitud.esSoloLocal()
            ? modeloLocal
            : solicitud.pideLlm() ? modelos.externo().orElse(null) : modelo;
    if (elegido == null) {
      // Pidieron el modelo de lenguaje y ya no está configurado: no se cambia al local en silencio.
      solicitud.marcarError(
          "No hay un modelo de lenguaje configurado en este ambiente. Pide el análisis con el"
              + " modelo local por reglas.",
          "LLM_NO_DISPONIBLE");
      avisar(solicitud, TipoNotificacion.ANALISIS_ERROR);
      return Optional.empty();
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
   * Llama al modelo y valida su respuesta; si el JSON no sirve lo intenta una vez más. Con un
   * modelo de lenguaje, antes de llamar busca la misma entrada en la caché y revisa el presupuesto.
   *
   * @return los hallazgos con el modelo que los generó, o vacío si no se logró
   */
  private Optional<Generado> generar(Long idSolicitud, Preparado preparado) {
    ModeloIA elegido = preparado.modelo();
    PeticionAnalisis peticion = preparado.peticion();
    if ("REGLAS".equals(elegido.tipo())) {
      try {
        RespuestaModelo respuesta = elegido.analizar(peticion);
        return Optional.of(
            new Generado(
                lector.leer(respuesta.contenido()), respuesta.modelo(), null, false, false));
      } catch (RuntimeException fallo) {
        LOG.warn("El modelo local falló para la solicitud {}", idSolicitud);
        return Optional.empty();
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
        return Optional.of(
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
      return Optional.of(new Generado(List.of(), modeloConfigurado, null, false, true));
    }
    for (int intento = 1; intento <= 2; intento++) {
      long inicio = System.nanoTime();
      try {
        RespuestaModelo respuesta = elegido.analizar(peticion);
        long duracion = (System.nanoTime() - inicio) / 1_000_000;
        Uso gasto =
            respuesta.uso() != null
                ? respuesta.uso()
                : new Uso(
                    muestra.tokensEstimados(),
                    EstimadorTokens.estimar(respuesta.contenido()),
                    0,
                    true);
        registrar(
            preparado,
            huella,
            gasto.entrada(),
            gasto.salida(),
            gasto.cache(),
            gasto.estimado(),
            "LLAMADA",
            duracion);
        List<HallazgoPropuesto> hallazgos = lector.leer(respuesta.contenido());
        uso.guardar(
            huella,
            respuesta.modelo(),
            instruccion.version(),
            respuesta.contenido(),
            gasto.entrada(),
            gasto.salida());
        return Optional.of(
            new Generado(
                hallazgos,
                respuesta.modelo(),
                lector.resumen(respuesta.contenido()),
                false,
                false));
      } catch (RespuestaInvalidaException invalida) {
        LOG.warn(
            "Respuesta inválida del modelo para la solicitud {} (intento {}): {}",
            idSolicitud,
            intento,
            invalida.getMessage());
      } catch (ModeloIAException fallo) {
        registrar(
            preparado, huella, 0, 0, 0, false, "ERROR", (System.nanoTime() - inicio) / 1_000_000);
        LOG.warn("El modelo falló para la solicitud {}: {}", idSolicitud, fallo.getMessage());
        break;
      }
    }
    return Optional.empty();
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
    PeticionAnalisis peticion = preparado.peticion();
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
   */
  private record Generado(
      List<HallazgoPropuesto> hallazgos,
      String modelo,
      String resumen,
      boolean desdeCache,
      boolean rechazado) {}

  /**
   * @param muestra lo que va al modelo de lenguaje; nula con el modelo local, que lee todo
   */
  private record Preparado(
      PeticionAnalisis peticion,
      PreparadorComentarios.Preparados preparados,
      ModeloIA modelo,
      Muestra muestra,
      Long idPeriodo) {}
}
