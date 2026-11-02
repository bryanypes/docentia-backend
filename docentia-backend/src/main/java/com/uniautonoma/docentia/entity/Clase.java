package com.uniautonoma.docentia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * Oferta de un curso en un periodo: el trío Campus {@code cod_curso} + {@code grupo} + {@code
 * jornada}.
 */
@Entity
@Table(name = "clases")
public class Clase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_periodo", nullable = false, updatable = false)
  private PeriodoAcademico periodo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_curso", nullable = false, updatable = false)
  private Curso curso;

  @Column(nullable = false, length = 10)
  private String grupo;

  @Column(nullable = false, length = 20)
  private String jornada;

  @Column(name = "tipo_curso", nullable = false, length = 20)
  private String tipoCurso;

  @Column(name = "inscritos_esperados")
  private Integer inscritosEsperados;

  @ManyToMany
  @JoinTable(
      name = "clases_docente",
      joinColumns = @JoinColumn(name = "clase_id"),
      inverseJoinColumns = @JoinColumn(name = "docente_id"))
  private Set<Usuario> docentes = new HashSet<>();

  protected Clase() {}

  public Clase(
      PeriodoAcademico periodo, Curso curso, String grupo, String jornada, String tipoCurso) {
    this.periodo = periodo;
    this.curso = curso;
    this.grupo = grupo;
    this.jornada = jornada;
    this.tipoCurso = tipoCurso;
  }

  public void agregarDocente(Usuario docente) {
    docentes.add(docente);
  }

  public void setInscritosEsperados(Integer inscritosEsperados) {
    this.inscritosEsperados = inscritosEsperados;
  }

  public Long getId() {
    return id;
  }

  public PeriodoAcademico getPeriodo() {
    return periodo;
  }

  public Curso getCurso() {
    return curso;
  }

  public String getGrupo() {
    return grupo;
  }

  public String getJornada() {
    return jornada;
  }

  public String getTipoCurso() {
    return tipoCurso;
  }

  public Integer getInscritosEsperados() {
    return inscritosEsperados;
  }

  public Set<Usuario> getDocentes() {
    return docentes;
  }
}
