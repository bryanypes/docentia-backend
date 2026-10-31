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
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "programas")
public class Programa {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idPrograma;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_facultad", nullable = false)
  private Facultad facultad;

  @Column(nullable = false, length = 120)
  private String nombre;

  @Column(nullable = false)
  private boolean esActivo = true;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaCreacion;

  protected Programa() {}

  public Programa(String nombre, Facultad facultad) {
    this.nombre = nombre;
    this.facultad = facultad;
  }

  public Long getIdPrograma() {
    return idPrograma;
  }

  public Facultad getFacultad() {
    return facultad;
  }

  public void setFacultad(Facultad facultad) {
    this.facultad = facultad;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public boolean getEsActivo() {
    return esActivo;
  }

  public void setEsActivo(boolean esActivo) {
    this.esActivo = esActivo;
  }

  public LocalDateTime getFechaCreacion() {
    return fechaCreacion;
  }
}
