package com.uniautonoma.docentia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "facultades")
public class Facultad {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idFacultad;

  @Column(nullable = false, length = 120)
  private String nombre;

  @Column(nullable = false)
  private boolean esActiva = true;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaCreacion;

  protected Facultad() {}

  public Facultad(String nombre) {
    this.nombre = nombre;
  }

  public Long getIdFacultad() {
    return idFacultad;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public boolean getEsActiva() {
    return esActiva;
  }

  public void setEsActiva(boolean esActiva) {
    this.esActiva = esActiva;
  }

  public LocalDateTime getFechaCreacion() {
    return fechaCreacion;
  }
}
