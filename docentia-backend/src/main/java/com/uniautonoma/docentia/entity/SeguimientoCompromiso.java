package com.uniautonoma.docentia.entity;

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
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

/** Cambio de estado de un compromiso, con su fecha y su autor. */
@Entity
@Table(name = "seguimiento_compromisos")
public class SeguimientoCompromiso {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idSeguimiento;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_compromiso", nullable = false, updatable = false)
  private Compromiso compromiso;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoCompromiso estado;

  @Column(nullable = false, length = 500)
  private String observacion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
  private Usuario usuario;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fecha;

  protected SeguimientoCompromiso() {}

  public SeguimientoCompromiso(
      Compromiso compromiso, EstadoCompromiso estado, String observacion, Usuario usuario) {
    this.compromiso = compromiso;
    this.estado = estado;
    this.observacion = observacion;
    this.usuario = usuario;
  }

  public EstadoCompromiso getEstado() {
    return estado;
  }

  public String getObservacion() {
    return observacion;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }
}
