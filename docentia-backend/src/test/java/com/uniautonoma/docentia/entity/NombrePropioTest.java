package com.uniautonoma.docentia.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NombrePropioTest {

  @Test
  void loQueLlegaTodoEnMayusculasOMinusculasQuedaConMayusculaInicial() {
    assertThat(NombrePropio.normalizar("ADMINISTRACION DE EMPRESAS PROFESIONAL"))
        .isEqualTo("Administración de Empresas Profesional");
    assertThat(NombrePropio.normalizar("FINANZAS Y NEGOCIOS INTERNACIONALES"))
        .isEqualTo("Finanzas y Negocios Internacionales");
    assertThat(NombrePropio.normalizar("DESARROLLO DE APLICACIONES MÓVILES"))
        .isEqualTo("Desarrollo de Aplicaciones Móviles");
    assertThat(NombrePropio.normalizar("  karen   verónica mancilla  "))
        .isEqualTo("Karen Verónica Mancilla");
    assertThat(NombrePropio.normalizar("ELECTIVA II (OPTATIVA)"))
        .isEqualTo("Electiva II (Optativa)");
    assertThat(NombrePropio.normalizar("DE LA CRUZ")).isEqualTo("De la Cruz");
    assertThat(NombrePropio.normalizar("INGENIERIA ELECTRONICA")).isEqualTo("Ingeniería Electrónica");
  }

  @Test
  void unNombreYaBienEscritoNoCambia() {
    assertThat(NombrePropio.normalizar("Ingeniería de Software"))
        .isEqualTo("Ingeniería de Software");
    assertThat(NombrePropio.normalizar("ELECTIVA I (Optativa)")).isEqualTo("ELECTIVA I (Optativa)");
    assertThat(NombrePropio.normalizar("Estudiante Demo 5")).isEqualTo("Estudiante Demo 5");
    assertThat(NombrePropio.normalizar("DEMO-101")).isEqualTo("Demo-101");
    assertThat(NombrePropio.normalizar("Gestion Publica")).isEqualTo("Gestion Publica");
    assertThat(NombrePropio.normalizar(null)).isNull();
  }
}
