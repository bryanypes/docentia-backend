package com.uniautonoma.docentia.entity;

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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

/** Un intento de cargar el CSV de evaluación de estudiantes, exitoso o rechazado. */
@Entity
@Table(name = "importaciones")
public class Importacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idImportacion;

  @Column(nullable = false, length = 255, updatable = false)
  private String nombreArchivo;

  @Column(nullable = false, length = 64, updatable = false)
  private String hashSha256;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_periodo", nullable = false, updatable = false)
  private PeriodoAcademico periodo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
  private Usuario usuario;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaImportacion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ResultadoImportacion resultado;

  @Column(length = 20)
  private String codificacion;

  @Column(length = 1)
  private String delimitador;

  @Column(nullable = false)
  private int filasProcesadas;

  @Column(nullable = false)
  private int filasConError;

  @Column(nullable = false)
  private int docentesAfectados;

  @Column(nullable = false)
  private int notasNuevas;

  @Column(nullable = false)
  private int notasCorregidas;

  @Column(nullable = false)
  private int notasSinCambios;

  @Column(nullable = false)
  private int comentariosNuevos;

  @Column(nullable = false)
  private int comentariosRepetidos;

  protected Importacion() {}

  public Importacion(
      String nombreArchivo,
      String hashSha256,
      PeriodoAcademico periodo,
      Usuario usuario,
      ResultadoImportacion resultado,
      String codificacion,
      String delimitador,
      int filasProcesadas,
      int filasConError) {
    this.nombreArchivo = nombreArchivo;
    this.hashSha256 = hashSha256;
    this.periodo = periodo;
    this.usuario = usuario;
    this.resultado = resultado;
    this.codificacion = codificacion;
    this.delimitador = delimitador;
    this.filasProcesadas = filasProcesadas;
    this.filasConError = filasConError;
  }

  /** Anota lo que la carga cambió una vez guardadas todas las filas. */
  public void registrarTotales(
      int docentesAfectados,
      int notasNuevas,
      int notasCorregidas,
      int notasSinCambios,
      int comentariosNuevos,
      int comentariosRepetidos) {
    this.docentesAfectados = docentesAfectados;
    this.notasNuevas = notasNuevas;
    this.notasCorregidas = notasCorregidas;
    this.notasSinCambios = notasSinCambios;
    this.comentariosNuevos = comentariosNuevos;
    this.comentariosRepetidos = comentariosRepetidos;
  }

  public Long getIdImportacion() {
    return idImportacion;
  }

  public String getNombreArchivo() {
    return nombreArchivo;
  }

  public String getHashSha256() {
    return hashSha256;
  }

  public PeriodoAcademico getPeriodo() {
    return periodo;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public LocalDateTime getFechaImportacion() {
    return fechaImportacion;
  }

  public ResultadoImportacion getResultado() {
    return resultado;
  }

  public String getCodificacion() {
    return codificacion;
  }

  public String getDelimitador() {
    return delimitador;
  }

  public int getFilasProcesadas() {
    return filasProcesadas;
  }

  public int getFilasConError() {
    return filasConError;
  }

  public int getDocentesAfectados() {
    return docentesAfectados;
  }

  public int getNotasNuevas() {
    return notasNuevas;
  }

  public int getNotasCorregidas() {
    return notasCorregidas;
  }

  public int getNotasSinCambios() {
    return notasSinCambios;
  }

  public int getComentariosNuevos() {
    return comentariosNuevos;
  }

  public int getComentariosRepetidos() {
    return comentariosRepetidos;
  }
}
