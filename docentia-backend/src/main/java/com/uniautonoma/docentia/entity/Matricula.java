package com.uniautonoma.docentia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Inscripción de un estudiante a una clase del periodo. Baja lógica con {@code es_activa}. */
@Entity
@Table(name = "matriculas")
public class Matricula {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "clase_id", nullable = false, updatable = false)
  private Clase clase;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "estudiante_id", nullable = false, updatable = false)
  private Usuario estudiante;

  @Column(nullable = false)
  private boolean esActiva = true;

  protected Matricula() {}

  public Matricula(Clase clase, Usuario estudiante) {
    this.clase = clase;
    this.estudiante = estudiante;
  }

  public Long getId() {
    return id;
  }

  public Clase getClase() {
    return clase;
  }

  public Usuario getEstudiante() {
    return estudiante;
  }

  public boolean getEsActiva() {
    return esActiva;
  }

  public void setEsActiva(boolean esActiva) {
    this.esActiva = esActiva;
  }
}
