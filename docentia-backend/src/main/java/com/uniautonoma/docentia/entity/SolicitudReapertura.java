package com.uniautonoma.docentia.entity;

import com.uniautonoma.docentia.exception.ReglaNegocioException;
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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

/**
 * Ticket para corregir una autoevaluación o una coevaluación ya enviada. La pide quien envió el
 * formulario, la verifica su superior y la atiende la administración reabriendo el formulario.
 */
@Entity
@Table(name = "solicitudes_reapertura")
public class SolicitudReapertura {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_formulario", nullable = false, updatable = false)
  private EvaluacionFormulario formulario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_solicitante", nullable = false, updatable = false)
  private Usuario solicitante;

  @Column(nullable = false, length = 500, updatable = false)
  private String motivo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 25)
  private EstadoSolicitudReapertura estado;

  /** Rol que debe verificarla; nulo si no hay superior y va directo a la administración. */
  @Enumerated(EnumType.STRING)
  @Column(name = "rol_aprobador", length = 20, updatable = false)
  private Rol rolAprobador;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_aprobador")
  private Usuario aprobador;

  @Column(name = "respuesta_aprobador", length = 500)
  private String respuestaAprobador;

  private LocalDateTime fechaAprobacion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_administrador")
  private Usuario administrador;

  @Column(name = "respuesta_administrador", length = 500)
  private String respuestaAdministrador;

  private LocalDateTime fechaAtencion;

  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaSolicitud;

  @Version private long version;

  protected SolicitudReapertura() {}

  public SolicitudReapertura(
      EvaluacionFormulario formulario,
      Usuario solicitante,
      String motivo,
      Rol rolAprobador,
      LocalDateTime ahora) {
    this.formulario = formulario;
    this.solicitante = solicitante;
    this.motivo = motivo;
    this.rolAprobador = rolAprobador;
    this.fechaSolicitud = ahora;
    this.estado =
        rolAprobador == null
            ? EstadoSolicitudReapertura.APROBADA
            : EstadoSolicitudReapertura.PENDIENTE_APROBACION;
  }

  /** Decisión del superior. */
  public void decidirAprobacion(
      boolean aprobada, Usuario quien, String respuesta, LocalDateTime ahora) {
    if (estado != EstadoSolicitudReapertura.PENDIENTE_APROBACION) {
      throw new ReglaNegocioException("Esta solicitud ya fue verificada");
    }
    this.aprobador = quien;
    this.respuestaAprobador = respuesta;
    this.fechaAprobacion = ahora;
    this.estado =
        aprobada ? EstadoSolicitudReapertura.APROBADA : EstadoSolicitudReapertura.RECHAZADA;
  }

  /** La administración la atiende: reabre el formulario o la rechaza. */
  public void atender(boolean reabrir, Usuario quien, String respuesta, LocalDateTime ahora) {
    if (estado != EstadoSolicitudReapertura.APROBADA) {
      throw new ReglaNegocioException(
          estado == EstadoSolicitudReapertura.PENDIENTE_APROBACION
              ? "La solicitud todavía no tiene la verificación del superior"
              : "Esta solicitud ya fue atendida");
    }
    this.administrador = quien;
    this.respuestaAdministrador = respuesta;
    this.fechaAtencion = ahora;
    this.estado =
        reabrir ? EstadoSolicitudReapertura.ATENDIDA : EstadoSolicitudReapertura.RECHAZADA;
  }

  public boolean estaAbierta() {
    return estado == EstadoSolicitudReapertura.PENDIENTE_APROBACION
        || estado == EstadoSolicitudReapertura.APROBADA;
  }

  public Long getId() {
    return id;
  }

  public EvaluacionFormulario getFormulario() {
    return formulario;
  }

  public Usuario getSolicitante() {
    return solicitante;
  }

  public String getMotivo() {
    return motivo;
  }

  public EstadoSolicitudReapertura getEstado() {
    return estado;
  }

  public Rol getRolAprobador() {
    return rolAprobador;
  }

  public Usuario getAprobador() {
    return aprobador;
  }

  public String getRespuestaAprobador() {
    return respuestaAprobador;
  }

  public LocalDateTime getFechaAprobacion() {
    return fechaAprobacion;
  }

  public Usuario getAdministrador() {
    return administrador;
  }

  public String getRespuestaAdministrador() {
    return respuestaAdministrador;
  }

  public LocalDateTime getFechaAtencion() {
    return fechaAtencion;
  }

  public LocalDateTime getFechaSolicitud() {
    return fechaSolicitud;
  }
}
