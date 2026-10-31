package com.uniautonoma.docentia.service.importacion;

import static com.uniautonoma.docentia.service.importacion.CampoImportacion.COMENTARIO;
import static com.uniautonoma.docentia.service.importacion.CampoImportacion.COMPROMISO_INSTITUCIONAL;
import static com.uniautonoma.docentia.service.importacion.CampoImportacion.CORREO_DOCENTE;
import static com.uniautonoma.docentia.service.importacion.CampoImportacion.DOMINIO_DISCIPLINAR;
import static com.uniautonoma.docentia.service.importacion.CampoImportacion.EVALUACION_APRENDIZAJE;
import static com.uniautonoma.docentia.service.importacion.CampoImportacion.METODOLOGIA_ENSENANZA;
import static com.uniautonoma.docentia.service.importacion.CampoImportacion.PLANEACION_ACADEMICA;
import static org.assertj.core.api.Assertions.assertThat;

import com.uniautonoma.docentia.service.importacion.ResolutorMapeo.OrigenMapeo;
import com.uniautonoma.docentia.service.importacion.ResolutorMapeo.Plantilla;
import com.uniautonoma.docentia.service.importacion.ResolutorMapeo.ResultadoMapeo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ResolutorMapeoTest {

  private static final List<String> COLUMNAS_POWER_BI =
      List.of("Email", "Dom.", "Plan.", "Metod.", "Eval.", "Comp.", "Obs");

  private final ResolutorMapeo resolutor = new ResolutorMapeo();

  @Test
  void sinMapeoReconoceLasColumnasPorSuNombreSinTildesNiMayusculas() {
    ResultadoMapeo resultado =
        resolver(
            List.of("CORREO DOCENTE", "Dominio_Disciplinar", "Planeacion academica", "Nombre"),
            null);

    assertThat(resultado.origen()).isEqualTo(OrigenMapeo.AUTOMATICO);
    assertThat(resultado.mapeo())
        .containsEntry("CORREO DOCENTE", CORREO_DOCENTE)
        .containsEntry("Dominio_Disciplinar", DOMINIO_DISCIPLINAR)
        .containsEntry("Planeacion academica", PLANEACION_ACADEMICA)
        .doesNotContainKey("Nombre");
    assertThat(resultado.obligatoriosSinMapear())
        .containsExactly(METODOLOGIA_ENSENANZA, EVALUACION_APRENDIZAJE, COMPROMISO_INSTITUCIONAL);
  }

  @Test
  void elMapeoManualReemplazaLaSugerencia() {
    ResultadoMapeo resultado = resolver(COLUMNAS_POWER_BI, mapeoPowerBi());

    assertThat(resultado.origen()).isEqualTo(OrigenMapeo.MANUAL);
    assertThat(resultado.obligatoriosSinMapear()).isEmpty();
    assertThat(resultado.errores()).isEmpty();
  }

  @Test
  void reportaColumnasInexistentesYCamposRepetidos() {
    Map<String, CampoImportacion> mapeo = new LinkedHashMap<>(mapeoPowerBi());
    mapeo.put("No existe", COMENTARIO);
    mapeo.put("Obs", DOMINIO_DISCIPLINAR);

    ResultadoMapeo resultado = resolver(COLUMNAS_POWER_BI, mapeo);

    assertThat(resultado.errores())
        .containsExactlyInAnyOrder(
            "La columna \"No existe\" del mapeo no está en el archivo",
            "El campo Dominio disciplinar está asignado a dos columnas: \"Dom.\" y \"Obs\"");
  }

  @Test
  void unaPlantillaConLasMismasColumnasSeReutilizaSola() {
    Plantilla plantilla = new Plantilla("Power BI 2026-1", COLUMNAS_POWER_BI, mapeoPowerBi());

    ResultadoMapeo resultado =
        resolutor.resolver(COLUMNAS_POWER_BI, null, Optional.empty(), List.of(plantilla));

    assertThat(resultado.origen()).isEqualTo(OrigenMapeo.PLANTILLA);
    assertThat(resultado.nombrePlantilla()).isEqualTo("Power BI 2026-1");
    assertThat(resultado.obligatoriosSinMapear()).isEmpty();
  }

  @Test
  void siLasColumnasCambiaronDiceQueFaltaYQueEsNuevo() {
    Plantilla plantilla = new Plantilla("Power BI 2026-1", COLUMNAS_POWER_BI, mapeoPowerBi());
    List<String> columnasNuevas =
        List.of("Email", "Dom.", "Plan.", "Metodología", "Eval.", "Comp.", "Obs");

    ResultadoMapeo resultado =
        resolutor.resolver(columnasNuevas, null, Optional.of(plantilla), List.of());

    assertThat(resultado.columnasFaltantes()).containsExactly("Metod.");
    assertThat(resultado.columnasNuevas()).containsExactly("Metodología");
    assertThat(resultado.obligatoriosSinMapear()).containsExactly(METODOLOGIA_ENSENANZA);
    // Con otras columnas no se reutiliza sola: hay que nombrarla.
    assertThat(
            resolutor.resolver(columnasNuevas, null, Optional.empty(), List.of(plantilla)).origen())
        .isEqualTo(OrigenMapeo.AUTOMATICO);
  }

  private ResultadoMapeo resolver(List<String> columnas, Map<String, CampoImportacion> mapeo) {
    return resolutor.resolver(columnas, mapeo, Optional.empty(), List.of());
  }

  private Map<String, CampoImportacion> mapeoPowerBi() {
    Map<String, CampoImportacion> mapeo = new LinkedHashMap<>();
    mapeo.put("Email", CORREO_DOCENTE);
    mapeo.put("Dom.", DOMINIO_DISCIPLINAR);
    mapeo.put("Plan.", PLANEACION_ACADEMICA);
    mapeo.put("Metod.", METODOLOGIA_ENSENANZA);
    mapeo.put("Eval.", EVALUACION_APRENDIZAJE);
    mapeo.put("Comp.", COMPROMISO_INSTITUCIONAL);
    mapeo.put("Obs", COMENTARIO);
    return mapeo;
  }
}
