package com.uniautonoma.docentia.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniautonoma.docentia.dto.EstadoPromedio;
import com.uniautonoma.docentia.dto.PromedioPonderadoResponse;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PesosFuentes;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CalculadoraPromedioPonderadoTest {

  private final CalculadoraPromedioPonderado calculadora = new CalculadoraPromedioPonderado();

  @Test
  void ponderaEstudiantesCincuentaCoordinacionTreintaYVicerrectoriaVeinte() {
    PromedioPonderadoResponse promedio =
        calculadora.calcularPromedio(notas("4.0", "3.0", "5.0"), PesosFuentes.predeterminados());

    assertThat(promedio.estado()).isEqualTo(EstadoPromedio.COMPLETO);
    assertThat(promedio.valor()).isEqualByComparingTo("3.90");
    assertThat(promedio.fuentesPendientes()).isEmpty();
  }

  @Test
  void siFaltaLaNotaDeEstudiantesQuedaIncompletoYSinValor() {
    PromedioPonderadoResponse promedio =
        calculadora.calcularPromedio(notas(null, "3.0", "5.0"), PesosFuentes.predeterminados());

    assertThat(promedio.estado()).isEqualTo(EstadoPromedio.INCOMPLETO);
    assertThat(promedio.valor()).isNull();
    assertThat(promedio.fuentesPendientes()).containsExactly(FuenteEvaluacion.ESTUDIANTES);
  }

  @Test
  void siFaltaCualquierOtraFuenteTambienQuedaIncompleto() {
    assertThat(
            calculadora
                .calcularPromedio(notas("4.0", null, "5.0"), PesosFuentes.predeterminados())
                .fuentesPendientes())
        .containsExactly(FuenteEvaluacion.COORDINACION);
    assertThat(
            calculadora
                .calcularPromedio(notas("4.0", "3.0", null), PesosFuentes.predeterminados())
                .fuentesPendientes())
        .containsExactly(FuenteEvaluacion.VICERRECTORIA);
  }

  @Test
  void sinNingunaNotaListaLasTresFuentesComoPendientes() {
    PromedioPonderadoResponse promedio =
        calculadora.calcularPromedio(notas(null, null, null), PesosFuentes.predeterminados());

    assertThat(promedio.estado()).isEqualTo(EstadoPromedio.INCOMPLETO);
    assertThat(promedio.fuentesPendientes())
        .containsExactly(
            FuenteEvaluacion.ESTUDIANTES,
            FuenteEvaluacion.COORDINACION,
            FuenteEvaluacion.VICERRECTORIA);
  }

  @Test
  void usaLosPesosConfiguradosParaElPeriodo() {
    PesosFuentes pesos =
        new PesosFuentes(new BigDecimal("40"), new BigDecimal("40"), new BigDecimal("20"));

    PromedioPonderadoResponse promedio =
        calculadora.calcularPromedio(notas("4.0", "3.0", "5.0"), pesos);

    assertThat(promedio.valor()).isEqualByComparingTo("3.80");
  }

  @Test
  void redondeaADosDecimalesHaciaArribaEnElPuntoMedio() {
    // 4.33 * 0.5 + 3.70 * 0.3 + 4.10 * 0.2 = 4.095
    PromedioPonderadoResponse promedio =
        calculadora.calcularPromedio(notas("4.33", "3.70", "4.10"), PesosFuentes.predeterminados());

    assertThat(promedio.valor()).isEqualTo(new BigDecimal("4.10"));
  }

  @Test
  void unPesoEnCeroNoAportaAlPromedio() {
    PesosFuentes pesos =
        new PesosFuentes(BigDecimal.ZERO, new BigDecimal("60"), new BigDecimal("40"));

    PromedioPonderadoResponse promedio =
        calculadora.calcularPromedio(notas("1.0", "4.0", "5.0"), pesos);

    assertThat(promedio.valor()).isEqualByComparingTo("4.40");
  }

  private Map<FuenteEvaluacion, BigDecimal> notas(
      String estudiantes, String coordinacion, String vicerrectoria) {
    Map<FuenteEvaluacion, BigDecimal> notasPorFuente = new EnumMap<>(FuenteEvaluacion.class);
    agregar(notasPorFuente, FuenteEvaluacion.ESTUDIANTES, estudiantes);
    agregar(notasPorFuente, FuenteEvaluacion.COORDINACION, coordinacion);
    agregar(notasPorFuente, FuenteEvaluacion.VICERRECTORIA, vicerrectoria);
    return notasPorFuente;
  }

  private void agregar(
      Map<FuenteEvaluacion, BigDecimal> notasPorFuente, FuenteEvaluacion fuente, String nota) {
    if (nota != null) {
      notasPorFuente.put(fuente, new BigDecimal(nota));
    }
  }
}
