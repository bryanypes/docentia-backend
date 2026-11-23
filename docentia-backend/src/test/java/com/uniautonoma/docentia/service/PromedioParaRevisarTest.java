package com.uniautonoma.docentia.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.TipoNotificacion;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.service.notificacion.EventoNotificacion;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Una nota final por debajo de 4,0 deja al docente «Para revisar»: la vicerrectoría recibe un
 * aviso. Las categorías de la Resolución 074 no cambian.
 */
@RecordApplicationEvents
class PromedioParaRevisarTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;
  @Autowired private ApplicationEvents eventos;

  private Usuario docente;
  private PeriodoAcademico periodo;

  @BeforeEach
  void crearEscenario() {
    docente =
        escenario.crearDocente(
            "Nora Munoz",
            escenario.crearPrograma("Software", escenario.crearFacultad("Ingeniería")));
    periodo = escenario.crearPeriodoActivo("2026-2");
  }

  private void calificarTodo(String nota) {
    for (FuenteEvaluacion fuente : FuenteEvaluacion.values()) {
      escenario.registrarCalificacion(docente, periodo, fuente, nota);
    }
  }

  private List<TipoNotificacion> avisosALaVicerrectoria() {
    return eventos.stream(EventoNotificacion.class)
        .filter(evento -> evento.destino() == EventoNotificacion.Destino.VICERRECTORES)
        .map(EventoNotificacion::tipo)
        .toList();
  }

  @Test
  void conElPromedioCompletoPorDebajoDe4AvisaALaVicerrectoria() {
    calificarTodo("3.9");

    assertThat(avisosALaVicerrectoria()).containsOnly(TipoNotificacion.PROMEDIO_PARA_REVISAR);
  }

  @Test
  void con4OMasNoHayAviso() {
    calificarTodo("4.0");

    assertThat(avisosALaVicerrectoria()).isEmpty();
  }

  @Test
  void muyBajoSigueSiendoLaAlertaDeLaResolucion() {
    calificarTodo("1.5");

    assertThat(avisosALaVicerrectoria()).containsOnly(TipoNotificacion.ALERTA_INFORMATIVA);
  }
}
