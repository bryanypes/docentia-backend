package com.uniautonoma.docentia.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;

/** Pedido de análisis de los comentarios de un docente en un periodo, con su borrador. */
@Entity
@Table(name = "solicitudes_analisis")
public class SolicitudAnalisis {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idSolicitud;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_docente", nullable = false, updatable = false)
  private Usuario docente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_periodo", nullable = false, updatable = false)
  private PeriodoAcademico periodo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_solicitante", nullable = false, updatable = false)
  private Usuario solicitante;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoSolicitudAnalisis estado = EstadoSolicitudAnalisis.EN_COLA;

  @Column(length = 500)
  private String mensaje;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private FuenteComentarios fuente = FuenteComentarios.CSV;

  /** Detalle de INSUFICIENTE: POCAS_ENCUESTAS, SIN_COMENTARIOS o TODOS_EXCLUIDOS. */
  @Column(length = 30)
  private String motivo;

  /** LOCAL fuerza el modelo por reglas; nulo usa el proveedor configurado. */
  @Column(length = 10, updatable = false)
  private String modeloSolicitado;

  @Column(length = 300, updatable = false)
  private String motivoRegeneracion;

  private Integer encuestasCerradas;

  private Integer comentariosEnviados;

  private Integer comentariosFiltrados;

  @Column(nullable = false)
  private int intentos;

  @Column(length = 100)
  private String modelo;

  @Column(length = 40)
  private String versionInstruccion;

  private LocalDateTime fechaBorrador;

  @Column(length = 30)
  private String estadoBorrador;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaSolicitud;

  private LocalDateTime fechaInicio;

  private LocalDateTime fechaFin;

  @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("orden asc")
  private List<Hallazgo> hallazgos = new ArrayList<>();

  protected SolicitudAnalisis() {}

  public SolicitudAnalisis(Usuario docente, PeriodoAcademico periodo, Usuario solicitante) {
    this.docente = docente;
    this.periodo = periodo;
    this.solicitante = solicitante;
  }

  public SolicitudAnalisis(
      Usuario docente, PeriodoAcademico periodo, Usuario solicitante, FuenteComentarios fuente) {
    this(docente, periodo, solicitante);
    this.fuente = fuente;
  }

  /**
   * @param soloLocal fuerza el modelo local por reglas
   * @param motivoRegeneracion por qué se vuelve a generar; nulo si es la primera vez
   */
  public SolicitudAnalisis(
      Usuario docente,
      PeriodoAcademico periodo,
      Usuario solicitante,
      FuenteComentarios fuente,
      boolean soloLocal,
      String motivoRegeneracion) {
    this(docente, periodo, solicitante, fuente);
    this.modeloSolicitado = soloLocal ? "LOCAL" : null;
    this.motivoRegeneracion = motivoRegeneracion;
  }

  /** Error con un motivo que la interfaz puede distinguir (por ejemplo PRESUPUESTO_AGOTADO). */
  public void marcarError(String mensajeError, String motivoError) {
    marcarError(mensajeError);
    this.motivo = motivoError;
  }

  public boolean esSoloLocal() {
    return "LOCAL".equals(modeloSolicitado);
  }

  public String getMotivoRegeneracion() {
    return motivoRegeneracion;
  }

  /** Marca la solicitud como insuficiente indicando el motivo, para mostrar el estado preciso. */
  public void marcarInsuficiente(int encuestas, String mensaje, String motivoInsuficiencia) {
    marcarInsuficiente(encuestas, mensaje);
    this.motivo = motivoInsuficiencia;
  }

  public FuenteComentarios getFuente() {
    return fuente;
  }

  public String getMotivo() {
    return motivo;
  }

  public void registrarFiltrados(int filtrados) {
    this.comentariosFiltrados = filtrados;
  }

  public void marcarInsuficiente(int encuestas, String mensaje) {
    this.estado = EstadoSolicitudAnalisis.INSUFICIENTE;
    this.encuestasCerradas = encuestas;
    this.mensaje = mensaje;
    this.fechaFin = LocalDateTime.now();
  }

  public void marcarError(String mensaje) {
    this.estado = EstadoSolicitudAnalisis.ERROR;
    this.mensaje = mensaje;
    this.fechaFin = LocalDateTime.now();
  }

  /** Guarda el borrador generado. Queda pendiente de validación: el docente no lo ve todavía. */
  public void completar(
      List<Hallazgo> nuevosHallazgos,
      int encuestas,
      int comentarios,
      String modeloUsado,
      String version) {
    this.hallazgos.clear();
    nuevosHallazgos.forEach(
        hallazgo -> {
          hallazgo.asignarSolicitud(this);
          this.hallazgos.add(hallazgo);
        });
    this.estado = EstadoSolicitudAnalisis.COMPLETADO;
    this.encuestasCerradas = encuestas;
    this.comentariosEnviados = comentarios;
    this.modelo = modeloUsado;
    this.versionInstruccion = version;
    this.fechaBorrador = LocalDateTime.now();
    this.fechaFin = this.fechaBorrador;
    this.estadoBorrador = "PENDIENTE_VALIDACION";
    this.mensaje = null;
  }

  public Long getIdSolicitud() {
    return idSolicitud;
  }

  public Usuario getDocente() {
    return docente;
  }

  public PeriodoAcademico getPeriodo() {
    return periodo;
  }

  public Usuario getSolicitante() {
    return solicitante;
  }

  public EstadoSolicitudAnalisis getEstado() {
    return estado;
  }

  public String getMensaje() {
    return mensaje;
  }

  public Integer getEncuestasCerradas() {
    return encuestasCerradas;
  }

  public Integer getComentariosEnviados() {
    return comentariosEnviados;
  }

  public Integer getComentariosFiltrados() {
    return comentariosFiltrados;
  }

  public int getIntentos() {
    return intentos;
  }

  public String getModelo() {
    return modelo;
  }

  public String getVersionInstruccion() {
    return versionInstruccion;
  }

  public LocalDateTime getFechaBorrador() {
    return fechaBorrador;
  }

  public String getEstadoBorrador() {
    return estadoBorrador;
  }

  /** Cierra la revisión: desde aquí el docente ve los hallazgos incluidos. */
  public void publicarBorrador() {
    this.estadoBorrador = "VALIDADO";
  }

  /**
   * Regla de visibilidad: el docente solo puede ver el borrador cuando coordinación lo validó
   * completo (E4). Cualquier endpoint futuro para el docente debe consultar esto.
   */
  public boolean esVisibleParaDocente() {
    return estado == EstadoSolicitudAnalisis.COMPLETADO && "VALIDADO".equals(estadoBorrador);
  }

  public LocalDateTime getFechaSolicitud() {
    return fechaSolicitud;
  }

  public LocalDateTime getFechaInicio() {
    return fechaInicio;
  }

  public LocalDateTime getFechaFin() {
    return fechaFin;
  }

  public List<Hallazgo> getHallazgos() {
    return Collections.unmodifiableList(hallazgos);
  }
}
