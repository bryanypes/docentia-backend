package com.uniautonoma.docentia;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.security.UsuarioSesion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Base de las pruebas que necesitan el contexto completo; cada prueba se revierte al terminar. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BaseDatosPruebaConfig.class)
@Transactional
public abstract class PruebaIntegracion {

  @Autowired protected MockMvc mockMvc;
  @Autowired protected ObjectMapper objectMapper;

  protected String aJson(Object cuerpo) throws Exception {
    return objectMapper.writeValueAsString(cuerpo);
  }

  protected RequestPostProcessor comoVicerrector() {
    return authentication(
        new UsuarioSesion(
                9001L,
                "Vicerrectora de prueba",
                "vicerrectoria@prueba.co",
                Rol.VICERRECTOR,
                null,
                null)
            .aAutenticacion());
  }

  protected RequestPostProcessor comoCoordinador(Long idFacultad) {
    return authentication(
        new UsuarioSesion(
                9002L,
                "Coordinador de prueba",
                "coordinacion@prueba.co",
                Rol.COORDINADOR,
                idFacultad,
                null)
            .aAutenticacion());
  }

  protected RequestPostProcessor como(Usuario usuario) {
    return authentication(UsuarioSesion.desde(usuario).aAutenticacion());
  }
}
