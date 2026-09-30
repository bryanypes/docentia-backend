package com.uniautonoma.docentia;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** Base de las pruebas que necesitan el contexto completo; cada prueba se revierte al terminar. */
@SpringBootTest
@Import(BaseDatosPruebaConfig.class)
@Transactional
public abstract class PruebaIntegracion {}
