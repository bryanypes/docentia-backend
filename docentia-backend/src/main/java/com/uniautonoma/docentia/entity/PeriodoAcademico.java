package com.uniautonoma.docentia.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "periodos_academicos")
public class PeriodoAcademico {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idPeriodo;

  @Column(nullable = false, length = 20)
  private String nombre;

  @Column(nullable = false)
  private LocalDate fechaInicio;

  @Column(nullable = false)
  private LocalDate fechaFin;

  @Column(nullable = false)
  private boolean esActivo;

  @OneToMany(mappedBy = "periodo", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("fechaApertura asc")
  private List<EtapaPeriodo> etapas = new ArrayList<>();

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaCreacion;

  protected PeriodoAcademico() {}

  public PeriodoAcademico(String nombre, LocalDate fechaInicio, LocalDate fechaFin) {
    this.nombre = nombre;
    this.fechaInicio = fechaInicio;
    this.fechaFin = fechaFin;
  }

  /**
   * Fija las fechas de una etapa. Si la etapa ya existe se reprograma en vez de reemplazarla,
   * porque Hibernate inserta antes de borrar y chocaría con la unicidad por periodo y tipo.
   */
  public void programarEtapa(TipoEtapa tipoEtapa, LocalDate fechaApertura, LocalDate fechaCierre) {
    etapas.stream()
        .filter(etapa -> etapa.getTipoEtapa() == tipoEtapa)
        .findFirst()
        .ifPresentOrElse(
            etapa -> etapa.reprogramar(fechaApertura, fechaCierre),
            () -> etapas.add(new EtapaPeriodo(this, tipoEtapa, fechaApertura, fechaCierre)));
  }

  public void conservarSoloEtapas(Set<TipoEtapa> tiposVigentes) {
    etapas.removeIf(etapa -> !tiposVigentes.contains(etapa.getTipoEtapa()));
  }

  public Long getIdPeriodo() {
    return idPeriodo;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public LocalDate getFechaInicio() {
    return fechaInicio;
  }

  public void setFechaInicio(LocalDate fechaInicio) {
    this.fechaInicio = fechaInicio;
  }

  public LocalDate getFechaFin() {
    return fechaFin;
  }

  public void setFechaFin(LocalDate fechaFin) {
    this.fechaFin = fechaFin;
  }

  public boolean getEsActivo() {
    return esActivo;
  }

  public void setEsActivo(boolean esActivo) {
    this.esActivo = esActivo;
  }

  public List<EtapaPeriodo> getEtapas() {
    return Collections.unmodifiableList(etapas);
  }
}
