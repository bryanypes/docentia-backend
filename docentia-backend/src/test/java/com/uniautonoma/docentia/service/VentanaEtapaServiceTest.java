package com.uniautonoma.docentia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.uniautonoma.docentia.config.RelojConfig;
import com.uniautonoma.docentia.dto.VentanaEtapaResponse;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.exception.EtapaCerradaException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class VentanaEtapaServiceTest {

  private static final LocalDate APERTURA = LocalDate.of(2026, 10, 19);
  private static final LocalDate CIERRE = LocalDate.of(2026, 10, 30);

  @Test
  void laVentanaIncluyeLosDiasDeAperturaYCierre() {
    PeriodoAcademico periodo = periodoConRecoleccion();

    for (LocalDate dia : List.of(APERTURA, CIERRE, APERTURA.plusDays(3))) {
      assertThatCode(
              () ->
                  servicioEl(dia).exigirEtapaAbierta(periodo, TipoEtapa.RECOLECCION_CALIFICACIONES))
          .doesNotThrowAnyException();
    }
  }

  @Test
  void antesDeAbrirODespuesDeCerrarLaEtapaEstaCerradaYDiceCuandoAbre() {
    PeriodoAcademico periodo = periodoConRecoleccion();

    assertThatThrownBy(
            () ->
                servicioEl(APERTURA.minusDays(1))
                    .exigirEtapaAbierta(periodo, TipoEtapa.RECOLECCION_CALIFICACIONES))
        .isInstanceOfSatisfying(
            EtapaCerradaException.class,
            excepcion -> {
              assertThat(excepcion.getEtapa()).isEqualTo(TipoEtapa.RECOLECCION_CALIFICACIONES);
              assertThat(excepcion.getFechaApertura()).isEqualTo(APERTURA);
              assertThat(excepcion.getFechaCierre()).isEqualTo(CIERRE);
              assertThat(excepcion.getFechaActual()).isEqualTo(APERTURA.minusDays(1));
              assertThat(excepcion.getMessage())
                  .isEqualTo(
                      "La etapa Recolección de calificaciones del periodo 2026-2 está abierta del"
                          + " 2026-10-19 al 2026-10-30; hoy es 2026-10-18");
            });
    assertThatThrownBy(
            () ->
                servicioEl(CIERRE.plusDays(1))
                    .exigirEtapaAbierta(periodo, TipoEtapa.RECOLECCION_CALIFICACIONES))
        .isInstanceOf(EtapaCerradaException.class);
  }

  @Test
  void unaEtapaSinProgramarCuentaComoCerrada() {
    assertThatThrownBy(
            () ->
                servicioEl(APERTURA)
                    .exigirEtapaAbierta(periodoConRecoleccion(), TipoEtapa.FIRMA_ACTAS))
        .isInstanceOfSatisfying(
            EtapaCerradaException.class,
            excepcion -> {
              assertThat(excepcion.getFechaApertura()).isNull();
              assertThat(excepcion.getMessage())
                  .contains("no tiene fechas para la etapa Firma de actas");
            });
  }

  @Test
  void describeLasCuatroEtapasEnElOrdenDelProceso() {
    List<VentanaEtapaResponse> ventanas =
        servicioEl(APERTURA).describirVentanas(periodoConRecoleccion());

    assertThat(ventanas)
        .extracting(VentanaEtapaResponse::etapa)
        .containsExactly(
            TipoEtapa.RECOLECCION_CALIFICACIONES,
            TipoEtapa.REVISION_HALLAZGOS,
            TipoEtapa.AGENDA_CITAS,
            TipoEtapa.FIRMA_ACTAS);
    assertThat(ventanas)
        .extracting(VentanaEtapaResponse::abierta)
        .containsExactly(true, false, false, false);
  }

  private PeriodoAcademico periodoConRecoleccion() {
    PeriodoAcademico periodo =
        new PeriodoAcademico("2026-2", LocalDate.of(2026, 8, 3), LocalDate.of(2026, 12, 5));
    periodo.programarEtapa(TipoEtapa.RECOLECCION_CALIFICACIONES, APERTURA, CIERRE);
    return periodo;
  }

  private VentanaEtapaService servicioEl(LocalDate dia) {
    // Casi medianoche en Colombia: ya es el día siguiente en UTC y no debe contar.
    Clock reloj =
        Clock.fixed(
            dia.atTime(LocalTime.of(23, 30)).atZone(RelojConfig.ZONA_COLOMBIA).toInstant(),
            RelojConfig.ZONA_COLOMBIA);
    return new VentanaEtapaService(reloj);
  }
}
