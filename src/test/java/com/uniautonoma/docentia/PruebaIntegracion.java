package com.uniautonoma.docentia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
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
}
