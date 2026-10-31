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
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Mapeo de columnas de un coordinador, con nombre, para reutilizarlo. Guarda las columnas con que
 * se creó para avisar si el archivo del siguiente periodo cambió. columnas y mapeo van como JSON.
 */
@Entity
@Table(name = "plantillas_mapeo")
public class PlantillaMapeo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idPlantilla;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
  private Usuario usuario;

  @Column(nullable = false, length = 100)
  private String nombre;

  @Column(nullable = false, columnDefinition = "text")
  private String columnas;

  @Column(nullable = false, columnDefinition = "text")
  private String mapeo;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaCreacion;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime fechaActualizacion;

  protected PlantillaMapeo() {}

  public PlantillaMapeo(Usuario usuario, String nombre) {
    this.usuario = usuario;
    this.nombre = nombre;
  }

  public void reemplazar(String columnasJson, String mapeoJson) {
    this.columnas = columnasJson;
    this.mapeo = mapeoJson;
  }

  public Long getIdPlantilla() {
    return idPlantilla;
  }

  public String getNombre() {
    return nombre;
  }

  public String getColumnas() {
    return columnas;
  }

  public String getMapeo() {
    return mapeo;
  }

  public LocalDateTime getFechaActualizacion() {
    return fechaActualizacion;
  }
}
