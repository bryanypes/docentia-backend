package com.uniautonoma.docentia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import org.hibernate.annotations.UpdateTimestamp;

/** Notas que una fuente le asigna a un docente en un periodo. Hay una sola por fuente. */
@Entity
@Table(name = "calificaciones")
public class Calificacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idCalificacion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_docente", nullable = false, updatable = false)
  private Usuario docente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_periodo", nullable = false, updatable = false)
  private PeriodoAcademico periodo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_evaluador")
  private Usuario evaluador;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private FuenteEvaluacion fuente;

  @Embedded private NotasAspectos notas;

  @Column(length = 1000)
  private String observacion;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaRegistro;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime fechaActualizacion;

  protected Calificacion() {}

  public Calificacion(Usuario docente, PeriodoAcademico periodo, FuenteEvaluacion fuente) {
    this.docente = docente;
    this.periodo = periodo;
    this.fuente = fuente;
  }

  public void registrar(NotasAspectos notas, String observacion, Usuario evaluador) {
    this.notas = notas;
    this.observacion = observacion;
    this.evaluador = evaluador;
  }

  public Long getIdCalificacion() {
    return idCalificacion;
  }

  public Usuario getDocente() {
    return docente;
  }

  public PeriodoAcademico getPeriodo() {
    return periodo;
  }

  public Usuario getEvaluador() {
    return evaluador;
  }

  public FuenteEvaluacion getFuente() {
    return fuente;
  }

  public NotasAspectos getNotas() {
    return notas;
  }

  public String getObservacion() {
    return observacion;
  }

  public LocalDateTime getFechaActualizacion() {
    return fechaActualizacion;
  }
}
