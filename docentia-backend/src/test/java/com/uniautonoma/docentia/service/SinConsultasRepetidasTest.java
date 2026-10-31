package com.uniautonoma.docentia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/** Los listados principales hacen las mismas consultas con 3 docentes que con 12. */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class SinConsultasRepetidasTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;
  @Autowired private EntityManager entityManager;
  @Autowired private EntityManagerFactory fabrica;

  private Programa programa;
  private PeriodoAcademico periodo;
  private Usuario coordinador;

  @BeforeEach
  void crearEscenario() {
    Facultad facultad = escenario.crearFacultad("Ingeniería");
    programa = escenario.crearPrograma("Software", facultad);
    coordinador = escenario.crearCoordinador("Marta Ruiz", facultad);
    periodo = escenario.crearPeriodoActivo("2026-2");
  }

  private void crearDocentes(int desde, int cuantos) {
    for (int i = desde; i < desde + cuantos; i++) {
      Usuario docente = escenario.crearDocente("Docente Numero " + i, programa);
      escenario.registrarCalificacion(docente, periodo, FuenteEvaluacion.COORDINACION, "4.0");
    }
  }

  private long consultasDe(String ruta) throws Exception {
    entityManager.flush();
    entityManager.clear();
    Statistics estadisticas = fabrica.unwrap(SessionFactory.class).getStatistics();
    estadisticas.clear();
    mockMvc.perform(get(ruta).with(como(coordinador))).andExpect(status().isOk());
    return estadisticas.getPrepareStatementCount();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"/docentes", "/docentes/seguimiento", "/acciones-sugeridas", "/kpis/panel"})
  void laCantidadDeConsultasNoCreceConLosDocentes(String ruta) throws Exception {
    crearDocentes(0, 3);
    long conPocos = consultasDe(ruta);
    crearDocentes(3, 9);
    long conMuchos = consultasDe(ruta);

    assertThat(conMuchos).as(ruta).isLessThanOrEqualTo(conPocos);
  }
}
