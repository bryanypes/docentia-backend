package com.uniautonoma.docentia.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.uniautonoma.docentia.BaseDatosPruebaConfig;
import com.uniautonoma.docentia.EscenarioPrueba;
import com.uniautonoma.docentia.PruebaIntegracion;
import com.uniautonoma.docentia.dto.CompromisoActaRequest;
import com.uniautonoma.docentia.dto.FirmaActaRequest;
import com.uniautonoma.docentia.dto.NuevaActaRequest;
import com.uniautonoma.docentia.entity.Acta;
import com.uniautonoma.docentia.entity.Cita;
import com.uniautonoma.docentia.entity.DatosCongeladosActa;
import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.FranjaHoraria;
import com.uniautonoma.docentia.entity.FuenteEvaluacion;
import com.uniautonoma.docentia.entity.PeriodoAcademico;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.repository.ActaRepository;
import com.uniautonoma.docentia.repository.CitaRepository;
import com.uniautonoma.docentia.repository.FranjaHorariaRepository;
import com.uniautonoma.docentia.service.GeneradorPdfActa;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

/**
 * F2 de la 0.5.0: el acta guarda una copia de lo que imprime y, firmada, no cambia aunque cambien
 * el docente, su programa, el periodo o los pesos. La base la protege entera, con sus firmas y
 * compromisos.
 */
class ActaCongeladaTest extends PruebaIntegracion {

  @Autowired private EscenarioPrueba escenario;
  @Autowired private CitaRepository citaRepository;
  @Autowired private FranjaHorariaRepository franjaRepository;
  @Autowired private ActaRepository actaRepository;
  @Autowired private GeneradorPdfActa generadorPdf;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManager entityManager;

  private Facultad ingenieria;
  private Usuario coordinador;
  private Usuario docente;
  private Usuario vicerrector;
  private PeriodoAcademico periodo;

  @BeforeEach
  void crearEscenario() {
    ingenieria = escenario.crearFacultad("Facultad de Ingeniería");
    coordinador = escenario.crearCoordinador("Marta Ruiz", ingenieria);
    vicerrector = escenario.crearVicerrector("Elena Munoz");
    docente = escenario.crearDocente("Nora Munoz", escenario.crearPrograma("Software", ingenieria));
    docente.setCodigo("1061234567");
    periodo =
        escenario.abrirEtapa(
            escenario.crearPeriodoActivo("2026-2"),
            TipoEtapa.FIRMA_ACTAS,
            BaseDatosPruebaConfig.HOY,
            BaseDatosPruebaConfig.HOY);
    for (FuenteEvaluacion fuente : FuenteEvaluacion.values()) {
      escenario.registrarCalificacion(docente, periodo, fuente, "3.2");
    }
    FranjaHoraria franja =
        franjaRepository.saveAndFlush(
            new FranjaHoraria(
                coordinador, periodo, BaseDatosPruebaConfig.HOY.plusDays(1), LocalTime.of(9, 0)));
    citaRepository.saveAndFlush(new Cita(franja, docente));
  }

  @Test
  void elActaGuardaLaIdentificacionLaReunionYLosPesos() throws Exception {
    Long acta = crearActa();

    mockMvc
        .perform(get("/actas/{id}", acta).with(como(coordinador)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.identificacion.docente").value("Nora Munoz"))
        .andExpect(jsonPath("$.identificacion.documentoRotulo").value("Código"))
        .andExpect(jsonPath("$.identificacion.documento").value("1061234567"))
        .andExpect(jsonPath("$.identificacion.cargo").value("Docente del programa Software"))
        .andExpect(jsonPath("$.identificacion.programa").value("Software"))
        .andExpect(jsonPath("$.identificacion.escuela").value("Escuela de Ingeniería"))
        .andExpect(jsonPath("$.identificacion.periodo").value("2026-2"))
        .andExpect(jsonPath("$.identificacion.periodoInicio").isNotEmpty())
        .andExpect(
            jsonPath("$.identificacion.evaluador")
                .value("Coordinación de la Escuela de Ingeniería"))
        .andExpect(
            jsonPath("$.identificacion.reunionFecha")
                .value(BaseDatosPruebaConfig.HOY.plusDays(1).toString()))
        .andExpect(jsonPath("$.identificacion.reunionHoraInicio").value("09:00:00"))
        .andExpect(jsonPath("$.identificacion.pesoEstudiantes").value(50))
        .andExpect(jsonPath("$.identificacion.respuestasEstudiantes").value(0));
  }

  @Test
  void elDocenteNoVeCuantosEstudiantesRespondieron() throws Exception {
    Long acta = actaFirmada();

    mockMvc
        .perform(get("/actas/{id}", acta).with(como(docente)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.identificacion.programa").value("Software"))
        .andExpect(jsonPath("$.identificacion.respuestasEstudiantes").doesNotExist());
  }

  @Test
  void firmadaNoCambiaAunqueCambienElDocenteElPeriodoYLosPesos() throws Exception {
    Long acta = actaFirmada();
    Programa otro = escenario.crearPrograma("Datos", escenario.crearFacultad("Ciencias"));
    entityManager.flush();
    jdbcTemplate.update(
        "update usuarios set nombre_completo = 'Nora Munoz Cambiada', codigo = '999',"
            + " id_programa = ? where id_usuario = ?",
        otro.getIdPrograma(),
        docente.getIdUsuario());
    jdbcTemplate.update(
        "update periodos_academicos set nombre = '2026-X' where id_periodo = ?",
        periodo.getIdPeriodo());
    entityManager.clear();

    mockMvc
        .perform(get("/actas/{id}", acta).with(como(coordinador)))
        .andExpect(jsonPath("$.identificacion.docente").value("Nora Munoz"))
        .andExpect(jsonPath("$.identificacion.documento").value("1061234567"))
        .andExpect(jsonPath("$.identificacion.programa").value("Software"))
        .andExpect(jsonPath("$.identificacion.escuela").value("Escuela de Ingeniería"))
        .andExpect(jsonPath("$.identificacion.periodo").value("2026-2"));
    mockMvc
        .perform(get("/actas/registro").with(como(vicerrector)))
        .andExpect(jsonPath("$[0].nombreDocente").value("Nora Munoz"))
        .andExpect(jsonPath("$[0].programa").value("Software"))
        .andExpect(jsonPath("$[0].periodo").value("2026-2"));
  }

  @Test
  void laPrimeraFirmaCongelaLoQueSeFirma() throws Exception {
    Long acta = crearActa();
    agregarCompromiso(acta).andExpect(status().isCreated());
    firmar(acta, coordinador).andExpect(status().isOk());

    // Entre la primera y la segunda firma cambia el programa del docente.
    Programa otro = escenario.crearPrograma("Datos", ingenieria);
    entityManager.flush();
    jdbcTemplate.update(
        "update usuarios set id_programa = ? where id_usuario = ?",
        otro.getIdPrograma(),
        docente.getIdUsuario());
    entityManager.clear();

    firmar(acta, docente)
        .andExpect(jsonPath("$.estado").value("FIRMADA"))
        .andExpect(jsonPath("$.identificacion.programa").value("Software"));
    String texto = textoDelPdf(acta);
    assertThat(texto)
        .contains("Docente del programa Software")
        .doesNotContain("programa Datos")
        .contains("1061234567")
        .contains("Escuela de Ingeniería")
        .contains("estudiantes 50 %");
  }

  @Test
  void conUnaFirmaLaBaseNoDejaCambiarElContenido() throws Exception {
    Long acta = crearActa();
    agregarCompromiso(acta).andExpect(status().isCreated());
    firmar(acta, coordinador).andExpect(status().isOk());
    entityManager.flush();

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "update actas set programa_nombre = 'Otro' where id_acta = ?", acta))
        .hasMessageContaining("ya tiene una firma");
    assertThatThrownBy(
            () -> jdbcTemplate.update("update actas set promedio = 5 where id_acta = ?", acta))
        .hasMessageContaining("ya tiene una firma");
  }

  @Test
  void firmadaLaBaseRechazaCualquierCambioDeSusColumnas() throws Exception {
    Long acta = actaFirmada();
    entityManager.flush();

    for (String cambio :
        List.of(
            "docente_nombre = 'Otro'",
            "docente_documento = '1'",
            "programa_nombre = 'Otro'",
            "escuela_nombre = 'Otra'",
            "periodo_nombre = 'Otro'",
            "reunion_fecha = reunion_fecha + 1",
            "peso_estudiantes = 10",
            "respuestas_estudiantes = 99",
            "fortalezas = 'Otra'",
            "tipo = 'RETROALIMENTACION'",
            "umbral_compromisos = 1",
            "nota_estudiantes = 1",
            "observaciones = 'Otra'",
            "formato_codigo = 'X'",
            "estado = 'ANULADA', promedio = 1")) {
      assertThatThrownBy(
              () -> jdbcTemplate.update("update actas set " + cambio + " where id_acta = ?", acta))
          .as(cambio)
          .hasMessageContaining("inmutable");
    }
    // Lo único permitido es anularla al corregirla.
    assertThatCode(
            () ->
                jdbcTemplate.update("update actas set estado = 'ANULADA' where id_acta = ?", acta))
        .doesNotThrowAnyException();
  }

  @Test
  void firmadaSusFirmasYCompromisosNoCambianSalvoElSeguimiento() throws Exception {
    Long acta = actaFirmada();
    entityManager.flush();

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "update firmas_acta set nombre = 'Otro' where id_acta = ?", acta))
        .hasMessageContaining("firmas");
    assertThatThrownBy(() -> jdbcTemplate.update("delete from firmas_acta where id_acta = ?", acta))
        .hasMessageContaining("firmas");
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "update compromisos set descripcion = 'Otro' where id_acta = ?", acta))
        .hasMessageContaining("seguimiento");
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "update compromisos set fecha_limite = fecha_limite + 1 where id_acta = ?",
                    acta))
        .hasMessageContaining("seguimiento");
    assertThatThrownBy(() -> jdbcTemplate.update("delete from compromisos where id_acta = ?", acta))
        .hasMessageContaining("no se elimina");
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "insert into compromisos (id_docente, id_periodo, id_acta, descripcion,"
                        + " fecha_limite) values (?, ?, ?, 'Nuevo', current_date)",
                    docente.getIdUsuario(),
                    periodo.getIdPeriodo(),
                    acta))
        .hasMessageContaining("compromisos nuevos");
    // El seguimiento del compromiso sí se registra después de firmar.
    assertThatCode(
            () ->
                jdbcTemplate.update(
                    "update compromisos set estado = 'CUMPLIDO', observacion = 'Listo'"
                        + " where id_acta = ?",
                    acta))
        .doesNotThrowAnyException();
  }

  @Test
  void elHashCambiaConLosDatosCongeladosYElFormatoVaEnElMembrete() throws Exception {
    Long id = crearActa();
    Acta acta = actaRepository.findById(id).orElseThrow();
    String antes = generadorPdf.hashContenido(acta, "DOC-PRUEBA");
    DatosCongeladosActa d = acta.getDatosCongelados();

    acta.congelarDatos(
        new DatosCongeladosActa(
            d.docenteNombre(),
            d.docenteCorreo(),
            d.documentoRotulo(),
            d.docenteDocumento(),
            d.docenteCargo(),
            "Otro programa",
            d.escuelaNombre(),
            d.periodoNombre(),
            d.periodoInicio(),
            d.periodoFin(),
            d.evaluadorCargo(),
            d.reunionFecha(),
            d.reunionHoraInicio(),
            d.reunionHoraFin(),
            d.reunionAsistencia(),
            new BigDecimal("40"),
            new BigDecimal("40"),
            new BigDecimal("20"),
            d.respuestasEstudiantes(),
            List.of("Explica con claridad"),
            List.of(),
            "F-VA-012",
            "2"));

    assertThat(generadorPdf.hashContenido(acta, "DOC-PRUEBA")).isNotEqualTo(antes);
    String texto = texto(generadorPdf.generar(acta, "DOC-PRUEBA", "hash"));
    assertThat(texto)
        .contains("Formato F-VA-012 · Versión 2")
        .contains("Otro programa")
        .contains("estudiantes 40 %")
        .contains("Explica con claridad");
    entityManager.detach(acta);
  }

  private Long actaFirmada() throws Exception {
    Long acta = crearActa();
    agregarCompromiso(acta).andExpect(status().isCreated());
    firmar(acta, coordinador).andExpect(status().isOk());
    firmar(acta, docente).andExpect(jsonPath("$.estado").value("FIRMADA"));
    return acta;
  }

  private Long crearActa() throws Exception {
    String cuerpo =
        mockMvc
            .perform(
                post("/actas")
                    .with(como(coordinador))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(aJson(new NuevaActaRequest(docente.getIdUsuario()))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(cuerpo, "$.idActa")).longValue();
  }

  private ResultActions firmar(Long acta, Usuario firmante) throws Exception {
    return mockMvc.perform(
        post("/actas/{id}/firmas", acta)
            .with(como(firmante))
            .contentType(MediaType.APPLICATION_JSON)
            .content(aJson(new FirmaActaRequest(true))));
  }

  private ResultActions agregarCompromiso(Long acta) throws Exception {
    return mockMvc.perform(
        post("/actas/{id}/compromisos", acta)
            .with(como(coordinador))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                aJson(
                    new CompromisoActaRequest(
                        "Actualizar el plan de asignatura",
                        BaseDatosPruebaConfig.HOY.plusMonths(2)))));
  }

  private String textoDelPdf(Long acta) throws Exception {
    return texto(
        mockMvc
            .perform(get("/actas/{id}/pdf", acta).with(como(docente)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray());
  }

  private static String texto(byte[] pdf) throws Exception {
    PdfReader lector = new PdfReader(pdf);
    StringBuilder paginas = new StringBuilder();
    for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
      paginas.append(new PdfTextExtractor(lector).getTextFromPage(pagina));
    }
    return paginas.toString();
  }

  @SuppressWarnings("unused")
  private static Rol rolEvaluador() {
    return Rol.COORDINADOR;
  }
}
