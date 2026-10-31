package com.uniautonoma.docentia.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Corre sin la transacción de la prueba, como en producción (open-in-view apagado): así se nota si
 * un endpoint lee relaciones perezosas fuera de una transacción. Con la transacción de las demás
 * pruebas ese error queda oculto.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class VentanasSinTransaccionTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;
  @Autowired private TransactionTemplate transaccion;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void lasVentanasDelPeriodoActivoSeLeenFueraDeLaTransaccionDeLaPrueba() throws Exception {
    transaccion.executeWithoutResult(
        estado ->
            escenario.crearPeriodoActivoConRecoleccion(
                "VT-2026", BaseDatosPruebaConfig.HOY, BaseDatosPruebaConfig.HOY));

    mockMvc
        .perform(get("/periodos-academicos/activo/ventanas").with(comoVicerrector()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].abierta").value(true));
  }

  @AfterEach
  void borrarElPeriodo() {
    jdbcTemplate.update("delete from periodos_academicos where nombre = 'VT-2026'");
  }
}
