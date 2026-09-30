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
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

/** Compromiso de mejora que un docente adquiere a partir de la evaluación de un periodo. */
@Entity
@Table(name = "compromisos")
public class Compromiso {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idCompromiso;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_docente", nullable = false, updatable = false)
  private Usuario docente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_periodo", nullable = false, updatable = false)
  private PeriodoAcademico periodo;

  @Column(nullable = false, length = 500)
  private String descripcion;

  @Column(nullable = false)
  private LocalDate fechaLimite;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoCompromiso estado = EstadoCompromiso.VIGENTE;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaCreacion;

  protected Compromiso() {}

  public Compromiso(
      Usuario docente, PeriodoAcademico periodo, String descripcion, LocalDate fechaLimite) {
    this.docente = docente;
    this.periodo = periodo;
    this.descripcion = descripcion;
    this.fechaLimite = fechaLimite;
  }

  public void marcarCumplido() {
    this.estado = EstadoCompromiso.CUMPLIDO;
  }

  public Long getIdCompromiso() {
    return idCompromiso;
  }

  public PeriodoAcademico getPeriodo() {
    return periodo;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public LocalDate getFechaLimite() {
    return fechaLimite;
  }

  public EstadoCompromiso getEstado() {
    return estado;
  }
}
