package com.uniautonoma.docentia.service.analisis;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.uniautonoma.docentia.config.IaProperties;
import com.uniautonoma.docentia.dto.SolicitudAnalisisRequest;
import com.uniautonoma.docentia.exception.ServicioDeshabilitadoException;
import org.junit.jupiter.api.Test;

class AnalisisIaDeshabilitadoTest {

  @Test
  void conElAnalisisApagadoNoSeAceptanSolicitudes() {
    IaProperties apagado =
        new IaProperties(
            false, "simulado", null, null, 60, null, 10, new IaProperties.Worker(false, 1));
    // Con el interruptor apagado se rechaza antes de tocar la base, así que no hacen falta
    // repositorios.
    AnalisisIaService servicio =
        new AnalisisIaService(null, null, null, null, null, apagado, null, null, null);

    assertThatThrownBy(() -> servicio.solicitar(new SolicitudAnalisisRequest(1L, 1L), null))
        .isInstanceOf(ServicioDeshabilitadoException.class)
        .hasMessageContaining("deshabilitado");
  }
}
