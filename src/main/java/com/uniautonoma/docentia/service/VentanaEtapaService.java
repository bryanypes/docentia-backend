package com.uniautonoma.docentia.service;

import com.uniautonoma.docentia.dto.VentanaEtapaResponse;
import com.uniautonoma.docentia.entity.EtapaPeriodo;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.exception.EtapaCerradaException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Único punto donde se decide si una etapa del periodo está abierta. Toda acción que dependa del
 * calendario lo llama antes de hacer cambios: calificar y cargar notas (recolección), y más
 * adelante revisar hallazgos, agendar citas y firmar actas. Las fechas son inclusivas.
 */
@Service
public class VentanaEtapaService {

  private final Clock reloj;

  public VentanaEtapaService(Clock reloj) {
    this.reloj = reloj;
  }

  /**
   * Exige que la etapa esté abierta hoy en el periodo.
   *
   * @param periodo periodo en el que ocurre la acción
   * @param etapa etapa a la que pertenece la acción
   * @throws EtapaCerradaException si la etapa no está programada o hoy está fuera de su ventana
   */
  public void exigirEtapaAbierta(PeriodoAcademico periodo, TipoEtapa etapa) {
    LocalDate hoy = hoy();
    Optional<EtapaPeriodo> programada = buscarEtapa(periodo, etapa);
    if (programada.isEmpty()) {
      throw new EtapaCerradaException(
          "El periodo "
              + periodo.getNombre()
              + " no tiene fechas para la etapa "
              + etapa.getNombre()
              + "; la vicerrectoría debe programarla",
          etapa,
          null,
          null,
          hoy);
    }
    EtapaPeriodo ventana = programada.get();
    if (!estaDentro(ventana, hoy)) {
      throw new EtapaCerradaException(
          "La etapa "
              + etapa.getNombre()
              + " del periodo "
              + periodo.getNombre()
              + " está abierta del "
              + ventana.getFechaApertura()
              + " al "
              + ventana.getFechaCierre()
              + "; hoy es "
              + hoy,
          etapa,
          ventana.getFechaApertura(),
          ventana.getFechaCierre(),
          hoy);
    }
  }

  /**
   * Describe hoy las cuatro etapas de un periodo, para que la interfaz habilite o no cada acción.
   *
   * @param periodo periodo consultado
   * @return una entrada por etapa, en el orden del proceso
   */
  public List<VentanaEtapaResponse> describirVentanas(PeriodoAcademico periodo) {
    LocalDate hoy = hoy();
    return Arrays.stream(TipoEtapa.values())
        .map(
            etapa -> {
              Optional<EtapaPeriodo> programada = buscarEtapa(periodo, etapa);
              return new VentanaEtapaResponse(
                  etapa,
                  etapa.getNombre(),
                  programada.map(EtapaPeriodo::getFechaApertura).orElse(null),
                  programada.map(EtapaPeriodo::getFechaCierre).orElse(null),
                  programada.map(ventana -> estaDentro(ventana, hoy)).orElse(false));
            })
        .toList();
  }

  /** Fecha de hoy en Colombia según el reloj de la aplicación. */
  public LocalDate hoy() {
    return LocalDate.now(reloj);
  }

  private Optional<EtapaPeriodo> buscarEtapa(PeriodoAcademico periodo, TipoEtapa etapa) {
    return periodo.getEtapas().stream()
        .filter(programada -> programada.getTipoEtapa() == etapa)
        .findFirst();
  }

  private boolean estaDentro(EtapaPeriodo ventana, LocalDate fecha) {
    return !fecha.isBefore(ventana.getFechaApertura()) && !fecha.isAfter(ventana.getFechaCierre());
  }
}
