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

@Entity
@Table(name = "etapas_periodo")
public class EtapaPeriodo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idEtapaPeriodo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_periodo", nullable = false)
  private PeriodoAcademico periodo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private TipoEtapa tipoEtapa;

  @Column(nullable = false)
  private LocalDate fechaApertura;

  @Column(nullable = false)
  private LocalDate fechaCierre;

  protected EtapaPeriodo() {}

  EtapaPeriodo(
      PeriodoAcademico periodo,
      TipoEtapa tipoEtapa,
      LocalDate fechaApertura,
      LocalDate fechaCierre) {
    this.periodo = periodo;
    this.tipoEtapa = tipoEtapa;
    this.fechaApertura = fechaApertura;
    this.fechaCierre = fechaCierre;
  }

  void reprogramar(LocalDate fechaApertura, LocalDate fechaCierre) {
    this.fechaApertura = fechaApertura;
    this.fechaCierre = fechaCierre;
  }

  public Long getIdEtapaPeriodo() {
    return idEtapaPeriodo;
  }

  public TipoEtapa getTipoEtapa() {
    return tipoEtapa;
  }

  public LocalDate getFechaApertura() {
    return fechaApertura;
  }

  public LocalDate getFechaCierre() {
    return fechaCierre;
  }
}
