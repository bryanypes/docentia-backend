package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.InicioSesionRequest;
import com.uniautonoma.docentia.entity.AccionAuditoria;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.AuditoriaService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** La auditoría guarda hasta los intentos fallidos y la base no deja cambiarla. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditoriaTest extends PruebaIntegracion {

  @Autowired private AuditoriaService auditoria;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void unLoginFallidoQuedaRegistradoYSeFiltra() throws Exception {
    String correo = UUID.randomUUID() + "@uniautonoma.edu.co";
    mockMvc
        .perform(
            post("/sesiones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(aJson(new InicioSesionRequest(correo, "mala"))))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            get("/auditoria")
                .param("usuario", correo)
                .param("accion", "LOGIN")
                .with(comoVicerrector()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElementos").value(1))
        .andExpect(jsonPath("$.contenido[0].resultado").value("FALLO"))
        .andExpect(jsonPath("$.contenido[0].ip").exists());
  }

  @Test
  void laBaseRechazaCambiarOBorrar() {
    auditoria.registrar(
        AccionAuditoria.CAMBIO_PESOS,
        "PONDERACION",
        null,
        new UsuarioSesion(
            1L,
            "Ana",
            "ana@prueba.co",
            com.uniautonoma.docentia.entity.Rol.VICERRECTOR,
            null,
            null,
            false));

    assertThatThrownBy(() -> jdbcTemplate.update("update auditoria set resultado = 'FALLO'"))
        .hasMessageContaining("inmutable");
    assertThatThrownBy(() -> jdbcTemplate.update("delete from auditoria"))
        .hasMessageContaining("inmutable");
    assertThat(jdbcTemplate.queryForObject("select count(*) from auditoria", Long.class))
        .isPositive();
  }
}
