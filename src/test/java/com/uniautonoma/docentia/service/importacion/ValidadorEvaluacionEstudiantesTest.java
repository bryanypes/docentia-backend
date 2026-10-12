package com.uniautonoma.docentia.service.importacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniautonoma.docentia.entity.Facultad;
import com.uniautonoma.docentia.entity.Programa;
import com.uniautonoma.docentia.entity.Usuario;
import com.uniautonoma.docentia.service.importacion.ValidadorEvaluacionEstudiantes.DatosDocente;
import com.uniautonoma.docentia.service.importacion.ValidadorEvaluacionEstudiantes.ErrorFila;
import com.uniautonoma.docentia.service.importacion.ValidadorEvaluacionEstudiantes.ResultadoValidacion;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ValidadorEvaluacionEstudiantesTest {

  private static final String ENCABEZADO =
      "correo;dominio;planeacion;metodologia;evaluacion;compromiso;comentario\n";

  private final ValidadorEvaluacionEstudiantes validador = new ValidadorEvaluacionEstudiantes();
  private final Usuario ana = docente(1L, "ana@x.co");
  private final Usuario luis = docente(2L, "luis@x.co");

  @Test
  void agrupaPorDocenteYPromediaCadaAspectoADosDecimales() {
    ResultadoValidacion resultado =
        validar(
            "ana@x.co;4.5;4;3;5;4;Muy clara\n"
                + "ANA@x.co;4,0;4;3;5;4,333;\n"
                + "luis@x.co;3;3;3;3;3;\n");

    assertThat(resultado.esValido()).isTrue();
    assertThat(resultado.filasProcesadas()).isEqualTo(3);
    DatosDocente datosAna = resultado.docentes().get(0);
    assertThat(datosAna.docente()).isSameAs(ana);
    assertThat(datosAna.filas()).isEqualTo(2);
    assertThat(datosAna.notas().dominioDisciplinar()).isEqualByComparingTo("4.25");
    // (4 + 4.333) / 2 = 4.1665 -> 4.17
    assertThat(datosAna.notas().compromisoInstitucional()).isEqualByComparingTo("4.17");
    assertThat(datosAna.comentarios()).containsExactly("Muy clara");
  }

  @Test
  void reportaCadaProblemaConFilaColumnaValorYMotivo() {
    ResultadoValidacion resultado =
        validar(
            "ana@x.co;7;4;4;4;4;\n"
                + "nadie@x.co;4;4;4;4;4;\n"
                + "luis@x.co;4;;4;abc;4;\n"
                + ";4;4;4;4;4;\n");

    assertThat(resultado.esValido()).isFalse();
    assertThat(resultado.filasConError()).isEqualTo(4);
    assertThat(resultado.docentes()).isEmpty();
    assertThat(resultado.errores())
        .contains(
            new ErrorFila(2, "dominio", "7", "La nota debe estar entre 0 y 5"),
            new ErrorFila(3, "correo", "nadie@x.co", "Docente no encontrado"),
            new ErrorFila(4, "evaluacion", "abc", "No es un número"),
            new ErrorFila(5, "correo", "", "El correo del docente es obligatorio"));
    assertThat(resultado.errores())
        .anySatisfy(
            error -> {
              assertThat(error.fila()).isEqualTo(4);
              assertThat(error.columna()).isEqualTo("planeacion");
              assertThat(error.motivo()).startsWith("Falta la nota de Planeación académica");
            });
  }

  @Test
  void unaFilaSoloConComentarioEsValidaPeroUnaVaciaNo() {
    ResultadoValidacion resultado = validar("ana@x.co;;;;;;Puntual\nluis@x.co;;;;;;\n");

    assertThat(resultado.errores())
        .containsExactly(new ErrorFila(3, null, null, "La fila no tiene notas ni comentario"));
    assertThat(resultado.docentes())
        .singleElement()
        .satisfies(
            datos -> {
              assertThat(datos.notas()).isNull();
              assertThat(datos.comentarios()).containsExactly("Puntual");
            });
  }

  @Test
  void aceptaLosLimitesDeLaEscalaYRechazaSignosYMiles() {
    assertThat(validar("ana@x.co;0;5;5,0;0.0;2.5;\n").esValido()).isTrue();
    assertThat(ValidadorEvaluacionEstudiantes.leerNumero("-1")).isEmpty();
    assertThat(ValidadorEvaluacionEstudiantes.leerNumero("1.000,5")).isEmpty();
    assertThat(ValidadorEvaluacionEstudiantes.leerNumero("4,5")).contains(new BigDecimal("4.5"));
  }

  @Test
  void laHuellaDelComentarioIgnoraEspaciosPeroDistingueLaAparicion() {
    String una = ValidadorEvaluacionEstudiantes.huellaComentario("  Muy   bien ", 1);

    assertThat(una).isEqualTo(ValidadorEvaluacionEstudiantes.huellaComentario("Muy bien", 1));
    assertThat(una).isNotEqualTo(ValidadorEvaluacionEstudiantes.huellaComentario("Muy bien", 2));
    assertThat(una).hasSize(64);
  }

  private ResultadoValidacion validar(String filas) {
    TablaCsv tabla = new LectorCsv(100).leer((ENCABEZADO + filas).getBytes(StandardCharsets.UTF_8));
    Map<CampoImportacion, String> mapeo =
        Map.of(
            CampoImportacion.CORREO_DOCENTE, "correo",
            CampoImportacion.DOMINIO_DISCIPLINAR, "dominio",
            CampoImportacion.PLANEACION_ACADEMICA, "planeacion",
            CampoImportacion.METODOLOGIA_ENSENANZA, "metodologia",
            CampoImportacion.EVALUACION_APRENDIZAJE, "evaluacion",
            CampoImportacion.COMPROMISO_INSTITUCIONAL, "compromiso",
            CampoImportacion.COMENTARIO, "comentario");
    Map<String, Usuario> docentes = Map.of("ana@x.co", ana, "luis@x.co", luis);
    return validador.validar(tabla, mapeo, correo -> Optional.ofNullable(docentes.get(correo)));
  }

  private static Usuario docente(Long id, String correo) {
    Usuario docente =
        Usuario.crearDocente(
            correo, correo, "hash", new Programa("Software", new Facultad("Ingeniería")));
    ReflectionTestUtils.setField(docente, "idUsuario", id);
    return docente;
  }
}
