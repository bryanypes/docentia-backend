package com.uniautonoma.docentia.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Fortaleza u oportunidad de mejora del borrador, con las citas que la respaldan. */
@Entity
@Table(name = "hallazgos_analisis")
public class Hallazgo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idHallazgo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_solicitud", nullable = false, updatable = false)
  private SolicitudAnalisis solicitud;

  @Column(nullable = false)
  private int orden;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TipoHallazgo tipo;

  @Column(nullable = false, length = 1000)
  private String texto;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EstadoHallazgo estado = EstadoHallazgo.PENDIENTE_VALIDACION;

  @OneToMany(mappedBy = "hallazgo", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("idCita asc")
  private List<CitaHallazgo> citas = new ArrayList<>();

  protected Hallazgo() {}

  public Hallazgo(int orden, TipoHallazgo tipo, String texto) {
    this.orden = orden;
    this.tipo = tipo;
    this.texto = texto;
  }

  void asignarSolicitud(SolicitudAnalisis solicitud) {
    this.solicitud = solicitud;
  }

  public void agregarCita(CitaHallazgo cita) {
    cita.asignarHallazgo(this);
    citas.add(cita);
  }

  public Long getIdHallazgo() {
    return idHallazgo;
  }

  public SolicitudAnalisis getSolicitud() {
    return solicitud;
  }

  public int getOrden() {
    return orden;
  }

  public TipoHallazgo getTipo() {
    return tipo;
  }

  public String getTexto() {
    return texto;
  }

  public EstadoHallazgo getEstado() {
    return estado;
  }

  public List<CitaHallazgo> getCitas() {
    return Collections.unmodifiableList(citas);
  }
}
