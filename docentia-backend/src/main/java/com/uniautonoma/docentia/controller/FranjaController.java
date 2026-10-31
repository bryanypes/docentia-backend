package com.uniautonoma.docentia.controller;

import com.uniautonoma.docentia.dto.FranjaRequest;
import com.uniautonoma.docentia.dto.FranjaResponse;
import com.uniautonoma.docentia.dto.LoteFranjasRequest;
import com.uniautonoma.docentia.dto.ReporteLoteFranjasResponse;
import com.uniautonoma.docentia.entity.EstadoFranja;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.FranjaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Franjas de atención que el coordinador abre para las citas con docentes. */
@RestController
@PreAuthorize("hasRole('COORDINADOR')")
@RequestMapping("/franjas")
public class FranjaController {

  private final FranjaService franjaService;

  public FranjaController(FranjaService franjaService) {
    this.franjaService = franjaService;
  }

  /** Lista las franjas propias del periodo activo, con su reserva si la tienen. */
  @GetMapping
  public List<FranjaResponse> listar(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fecha,
      @RequestParam(required = false) EstadoFranja estado,
      @AuthenticationPrincipal UsuarioSesion sesion) {
    return franjaService.listar(fecha, estado, sesion);
  }

  /** Abre una franja de 30 minutos. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FranjaResponse abrir(
      @Valid @RequestBody FranjaRequest solicitud, @AuthenticationPrincipal UsuarioSesion sesion) {
    return franjaService.abrir(solicitud, sesion);
  }

  /** Abre o cierra varias franjas e informa cuáles se aplicaron y cuáles no. */
  @PostMapping("/lote")
  public ReporteLoteFranjasResponse aplicarLote(
      @Valid @RequestBody LoteFranjasRequest lote, @AuthenticationPrincipal UsuarioSesion sesion) {
    return franjaService.aplicarLote(lote, sesion);
  }

  /** Cierra una franja; 409 si tiene una cita reservada. */
  @PatchMapping("/{idFranja}/cierre")
  public FranjaResponse cerrar(
      @PathVariable Long idFranja, @AuthenticationPrincipal UsuarioSesion sesion) {
    return franjaService.cerrar(idFranja, sesion);
  }

  /** Elimina una franja que nunca tuvo citas. */
  @DeleteMapping("/{idFranja}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long idFranja, @AuthenticationPrincipal UsuarioSesion sesion) {
    franjaService.eliminar(idFranja, sesion);
  }
}
