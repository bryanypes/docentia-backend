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
import java.time.LocalTime;
import org.hibernate.annotations.CreationTimestamp;

/** Bloque de 30 minutos que un coordinador abre para atender citas. */
@Entity
@Table(name = "franjas_horarias")
public class FranjaHoraria {

  public static final int DURACION_MINUTOS = 30;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idFranja;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_coordinador", nullable = false, updatable = false)
  private Usuario coordinador;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_periodo", nullable = false, updatable = false)
  private PeriodoAcademico periodo;

  @Column(nullable = false, updatable = false)
  private LocalDate fecha;

  @Column(nullable = false, updatable = false)
  private LocalTime horaInicio;

  @Column(nullable = false, updatable = false)
  private LocalTime horaFin;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoFranja estado = EstadoFranja.ABIERTA;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaCreacion;

  protected FranjaHoraria() {}

  public FranjaHoraria(
      Usuario coordinador, PeriodoAcademico periodo, LocalDate fecha, LocalTime horaInicio) {
    this.coordinador = coordinador;
    this.periodo = periodo;
    this.fecha = fecha;
    this.horaInicio = horaInicio;
    this.horaFin = horaInicio.plusMinutes(DURACION_MINUTOS);
  }

  public void cerrar() {
    this.estado = EstadoFranja.CERRADA;
  }

  public boolean estaAbierta() {
    return estado == EstadoFranja.ABIERTA;
  }

  public Long getIdFranja() {
    return idFranja;
  }

  public Usuario getCoordinador() {
    return coordinador;
  }

  public PeriodoAcademico getPeriodo() {
    return periodo;
  }

  public LocalDate getFecha() {
    return fecha;
  }

  public LocalTime getHoraInicio() {
    return horaInicio;
  }

  public LocalTime getHoraFin() {
    return horaFin;
  }

  public EstadoFranja getEstado() {
    return estado;
  }
}
