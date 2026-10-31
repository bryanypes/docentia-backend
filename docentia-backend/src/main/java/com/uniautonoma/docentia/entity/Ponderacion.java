package com.uniautonoma.docentia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "ponderaciones")
public class Ponderacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idPonderacion;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_periodo", nullable = false, updatable = false)
  private PeriodoAcademico periodo;

  @Embedded private PesosFuentes pesos;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime fechaActualizacion;

  protected Ponderacion() {}

  public Ponderacion(PeriodoAcademico periodo, PesosFuentes pesos) {
    this.periodo = periodo;
    this.pesos = pesos;
  }

  public Long getIdPonderacion() {
    return idPonderacion;
  }

  public PeriodoAcademico getPeriodo() {
    return periodo;
  }

  public PesosFuentes getPesos() {
    return pesos;
  }

  public void setPesos(PesosFuentes pesos) {
    this.pesos = pesos;
  }
}
