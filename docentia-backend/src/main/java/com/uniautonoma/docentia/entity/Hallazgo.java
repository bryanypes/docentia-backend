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
import java.time.LocalDateTime;
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
  private EstadoHallazgo estado = EstadoHallazgo.SIN_CITA_VERIFICADA;

  @OneToMany(mappedBy = "hallazgo", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("idCita asc")
  private List<CitaHallazgo> citas = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_validador")
  private Usuario validador;

  private LocalDateTime fechaValidacion;

  protected Hallazgo() {}

  public Hallazgo(int orden, TipoHallazgo tipo, String texto) {
    this.orden = orden;
    this.tipo = tipo;
    this.texto = texto;
  }

  void asignarSolicitud(SolicitudAnalisis solicitud) {
    this.solicitud = solicitud;
  }

  /** Agrega una cita y recalcula el estado: sin ninguna cita verificada no se puede validar. */
  public void agregarCita(CitaHallazgo cita) {
    cita.asignarHallazgo(this);
    citas.add(cita);
    actualizarEstadoSegunCitas();
  }

  /** Deja el estado según las citas; un hallazgo sin citas también queda sin cita verificada. */
  public void actualizarEstadoSegunCitas() {
    if (estado != EstadoHallazgo.VALIDADO) {
      estado =
          tieneCitaVerificada()
              ? EstadoHallazgo.PENDIENTE_VALIDACION
              : EstadoHallazgo.SIN_CITA_VERIFICADA;
    }
  }

  public boolean tieneCitaVerificada() {
    return citas.stream().anyMatch(CitaHallazgo::isVerificada);
  }

  /**
   * Marca el hallazgo como válido.
   *
   * @throws IllegalStateException si no tiene ninguna cita verificada
   */
  public void validar(Usuario quien) {
    if (!tieneCitaVerificada()) {
      throw new IllegalStateException("El hallazgo no tiene una cita verificada");
    }
    if (estado != EstadoHallazgo.VALIDADO) {
      estado = EstadoHallazgo.VALIDADO;
      validador = quien;
      fechaValidacion = LocalDateTime.now();
    }
  }

  public Usuario getValidador() {
    return validador;
  }

  public LocalDateTime getFechaValidacion() {
    return fechaValidacion;
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
