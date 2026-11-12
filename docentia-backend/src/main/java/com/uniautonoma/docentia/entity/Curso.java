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

/** Asignatura identificada por el código Campus (`cod_curso`). */
@Entity
@Table(name = "cursos")
public class Curso {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 40)
  private String codigo;

  @Column(nullable = false, length = 200)
  private String nombre;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_programa", nullable = false)
  private Programa programa;

  @Column(nullable = false)
  private boolean esActiva = true;

  protected Curso() {}

  public Curso(String codigo, String nombre, Programa programa) {
    this.codigo = codigo;
    this.nombre = nombre;
    this.programa = programa;
  }

  public Long getId() {
    return id;
  }

  public String getCodigo() {
    return codigo;
  }

  public String getNombre() {
    return nombre;
  }

  public Programa getPrograma() {
    return programa;
  }

  public boolean getEsActiva() {
    return esActiva;
  }

  public void setEsActiva(boolean esActiva) {
    this.esActiva = esActiva;
  }
}
