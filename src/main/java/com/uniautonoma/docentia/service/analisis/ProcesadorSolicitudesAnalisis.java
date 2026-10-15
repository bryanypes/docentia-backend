package com.uniautonoma.docentia.service.analisis;

import com.uniautonoma.docentia.config.IaProperties;
import com.uniautonoma.docentia.entity.CitaHallazgo;
import com.uniautonoma.docentia.entity.EstadoSolicitudAnalisis;
import com.uniautonoma.docentia.entity.Hallazgo;
import com.uniautonoma.docentia.entity.SolicitudAnalisis;
import com.uniautonoma.docentia.repository.SolicitudAnalisisRepository;
import com.uniautonoma.docentia.service.analisis.LectorRespuestaAnalisis.HallazgoPropuesto;
import com.uniautonoma.docentia.service.analisis.LectorRespuestaAnalisis.RespuestaInvalidaException;
import com.uniautonoma.docentia.service.analisis.ModeloIA.ModeloIAException;
import com.uniautonoma.docentia.service.analisis.ModeloIA.PeticionAnalisis;
import com.uniautonoma.docentia.service.analisis.ModeloIA.RespuestaModelo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  static final String MENSAJE_ERROR =
      "No fue posible generar el análisis en este momento. Puedes volver a solicitarlo más tarde.";

  private static final Logger LOG = LoggerFactory.getLogger(ProcesadorSolicitudesAnalisis.class);

  private final SolicitudAnalisisRepository solicitudRepository;
  private final ProveedorComentarios proveedorComentarios;
  private final PreparadorComentarios preparador;
  private final InstruccionAnalisis instruccion;
  private final ModeloIA modelo;
  private final LectorRespuestaAnalisis lector;
  private final IaProperties propiedades;
  private final TransactionTemplate transaccion;
  private final JdbcTemplate jdbcTemplate;

  public ProcesadorSolicitudesAnalisis(
      SolicitudAnalisisRepository solicitudRepository,
      ProveedorComentarios proveedorComentarios,
      PreparadorComentarios preparador,
      InstruccionAnalisis instruccion,
      ModeloIA modelo,
      LectorRespuestaAnalisis lector,
      IaProperties propiedades,
      TransactionTemplate transaccion,
      JdbcTemplate jdbcTemplate) {
    this.solicitudRepository = solicitudRepository;
    this.proveedorComentarios = proveedorComentarios;
    this.preparador = preparador;
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
      Optional<PeticionAnalisis> peticion = transaccion.execute(estado -> preparar(idSolicitud));
      if (peticion.isPresent()) {
        Optional<Generado> generado = generar(idSolicitud, peticion.get());
        if (generado.isPresent()) {
          transaccion.executeWithoutResult(
              estado -> guardarBorrador(idSolicitud, peticion.get(), generado.get()));
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

  private Optional<PeticionAnalisis> preparar(Long idSolicitud) {
    SolicitudAnalisis solicitud = solicitudRepository.findById(idSolicitud).orElseThrow();
    Long idDocente = solicitud.getDocente().getIdUsuario();
    Long idPeriodo = solicitud.getPeriodo().getIdPeriodo();
    int encuestas = proveedorComentarios.contarEncuestasCerradas(idDocente, idPeriodo);
    if (encuestas < propiedades.minimoEncuestas()) {
      solicitud.marcarInsuficiente(
          encuestas,
          "evidencia insuficiente ("
              + encuestas
              + " de "
              + propiedades.minimoEncuestas()
              + " requeridas)");
      return Optional.empty();
    }
    PreparadorComentarios.Preparados preparados = preparador.preparar(solicitud);
    solicitud.registrarFiltrados(preparados.filtrados());
    if (preparados.enviados().isEmpty()) {
      solicitud.marcarInsuficiente(
          encuestas, "evidencia insuficiente: el filtro excluyó todos los comentarios");
      return Optional.empty();
    }
    return Optional.of(new PeticionAnalisis(instruccion.texto(), preparados.enviados()));
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

  private void guardarBorrador(Long idSolicitud, PeticionAnalisis peticion, Generado generado) {
    SolicitudAnalisis solicitud = solicitudRepository.findById(idSolicitud).orElseThrow();
    List<Hallazgo> hallazgos = new ArrayList<>();
    int orden = 1;
    for (HallazgoPropuesto propuesto : generado.hallazgos()) {
      Hallazgo hallazgo = new Hallazgo(orden++, propuesto.tipo(), propuesto.texto());
      propuesto
          .citas()
          .forEach(
              cita ->
                  hallazgo.agregarCita(new CitaHallazgo(cita.comentarioId(), cita.fragmento())));
      hallazgos.add(hallazgo);
    }
    int encuestas =
        proveedorComentarios.contarEncuestasCerradas(
            solicitud.getDocente().getIdUsuario(), solicitud.getPeriodo().getIdPeriodo());
    solicitud.completar(
        hallazgos,
        encuestas,
        peticion.comentarios().size(),
        generado.modelo(),
        instruccion.version());
  }

  private void marcarError(Long idSolicitud) {
    transaccion.executeWithoutResult(
        estado ->
            solicitudRepository
                .findById(idSolicitud)
                .filter(solicitud -> solicitud.getEstado() == EstadoSolicitudAnalisis.EN_PROCESO)
                .ifPresent(solicitud -> solicitud.marcarError(MENSAJE_ERROR)));
  }

  private record Generado(List<HallazgoPropuesto> hallazgos, String modelo) {}
}
