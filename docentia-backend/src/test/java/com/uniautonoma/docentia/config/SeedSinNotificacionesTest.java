package com.uniautonoma.docentia.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.service.notificacion.GeneradorRecordatorios;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Con el seed en su propia base: arrancar no notifica, y los recordatorios reales siguen. */
@ActiveProfiles({"test", "dev"})
@TestPropertySource(properties = "docentia.ia.worker.habilitado=false")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SeedSinNotificacionesTest extends PruebaIntegracion {

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private GeneradorRecordatorios recordatorios;

  private long contar(String tabla) {
    return jdbcTemplate.queryForObject("select count(*) from " + tabla, Long.class);
  }

  private List<String> tipos() {
    return jdbcTemplate.queryForList("select distinct tipo from notificaciones", String.class);
  }

  @Test
  void elSeedNoNotificaYSoloQuedanLosRecordatoriosReales() {
    assertThat(contar("usuarios")).isPositive();
    assertThat(contar("promedios_docente")).isPositive();
    assertThat(contar("notificaciones")).isZero();
    assertThat(contar("cola_correo")).isZero();

    recordatorios.generar();

    assertThat(tipos()).doesNotContain("VENTANA_ABIERTA").contains("COMPROMISO_VENCIDO");
  }
}
