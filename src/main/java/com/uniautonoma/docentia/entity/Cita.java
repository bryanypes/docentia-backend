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

/**
 * Cita de un docente en una franja. Nace ACTIVA y pasa a LIBERADA con un motivo; liberar a
 * propósito (F3) es solo llamar a {@link #liberar(MotivoLiberacion)} con LIBERADA_POR_DOCENTE.
 */
@Entity
@Table(name = "citas")
public class Cita {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idCita;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_franja", nullable = false, updatable = false)
  private FranjaHoraria franja;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_docente", nullable = false, updatable = false)
  private Usuario docente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_periodo", nullable = false, updatable = false)
  private PeriodoAcademico periodo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoCita estado = EstadoCita.ACTIVA;

  @Enumerated(EnumType.STRING)
  @Column(length = 30)
  private MotivoLiberacion motivoLiberacion;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaReserva;

  private LocalDateTime fechaLiberacion;

  protected Cita() {}

  public Cita(FranjaHoraria franja, Usuario docente) {
    this.franja = franja;
    this.docente = docente;
    this.periodo = franja.getPeriodo();
  }

  public void liberar(MotivoLiberacion motivo) {
    this.estado = EstadoCita.LIBERADA;
    this.motivoLiberacion = motivo;
    this.fechaLiberacion = LocalDateTime.now();
  }

  public Long getIdCita() {
    return idCita;
  }

  public FranjaHoraria getFranja() {
    return franja;
  }

  public Usuario getDocente() {
    return docente;
  }

  public PeriodoAcademico getPeriodo() {
    return periodo;
  }

  public EstadoCita getEstado() {
    return estado;
  }

  public MotivoLiberacion getMotivoLiberacion() {
    return motivoLiberacion;
  }

  public LocalDateTime getFechaReserva() {
    return fechaReserva;
  }

  public LocalDateTime getFechaLiberacion() {
    return fechaLiberacion;
  }
}
