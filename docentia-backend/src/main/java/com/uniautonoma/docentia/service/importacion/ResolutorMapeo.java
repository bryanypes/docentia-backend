package com.uniautonoma.docentia.service.importacion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Decide qué columna del archivo corresponde a cada campo esperado. Por orden de prioridad: el
 * mapeo que envía el usuario, la plantilla que nombra, una plantilla suya con exactamente las
 * mismas columnas, o lo que se reconoce por el nombre del encabezado.
 */
public class ResolutorMapeo {

  /**
   * Resuelve el mapeo para las columnas de un archivo.
   *
   * @param columnas encabezados del archivo
   * @param explicito mapeo columna → campo enviado por el usuario, o nulo
   * @param plantillaNombrada plantilla que el usuario pidió usar, o vacío
   * @param plantillasDelUsuario todas sus plantillas, para reconocer una con las mismas columnas
   * @return el mapeo, de dónde salió y los problemas encontrados
   */
  public ResultadoMapeo resolver(
      List<String> columnas,
      Map<String, CampoImportacion> explicito,
      Optional<Plantilla> plantillaNombrada,
      List<Plantilla> plantillasDelUsuario) {
    if (explicito != null) {
      return conMapeo(columnas, explicito, OrigenMapeo.MANUAL, null, List.of(), List.of());
    }
    if (plantillaNombrada.isPresent()) {
      return desdePlantilla(columnas, plantillaNombrada.get());
    }
    Optional<Plantilla> coincidente =
        plantillasDelUsuario.stream()
            .filter(plantilla -> mismasColumnas(plantilla.columnas(), columnas))
            .findFirst();
    if (coincidente.isPresent()) {
      return desdePlantilla(columnas, coincidente.get());
    }
    return conMapeo(
        columnas, sugerir(columnas), OrigenMapeo.AUTOMATICO, null, List.of(), List.of());
  }

  /** Asigna cada columna al primer campo libre que la reconoce por su nombre. */
  public static Map<String, CampoImportacion> sugerir(List<String> columnas) {
    Map<String, CampoImportacion> mapeo = new LinkedHashMap<>();
    for (String columna : columnas) {
      Arrays.stream(CampoImportacion.values())
          .filter(campo -> !mapeo.containsValue(campo) && campo.reconoce(columna))
          .findFirst()
          .ifPresent(campo -> mapeo.put(columna, campo));
    }
    return mapeo;
  }

  /** Campos obligatorios que ninguna columna cubre. */
  public static List<CampoImportacion> obligatoriosSinMapear(Map<String, CampoImportacion> mapeo) {
    return Arrays.stream(CampoImportacion.values())
        .filter(campo -> campo.isObligatorio() && !mapeo.containsValue(campo))
        .toList();
  }

  private ResultadoMapeo desdePlantilla(List<String> columnas, Plantilla plantilla) {
    List<String> faltantes =
        plantilla.columnas().stream().filter(columna -> !columnas.contains(columna)).toList();
    List<String> nuevas =
        columnas.stream().filter(columna -> !plantilla.columnas().contains(columna)).toList();
    // Solo se aplican las asignaciones cuyas columnas siguen en el archivo; las demás quedan como
    // campos sin mapear y el reporte explica qué cambió.
    Map<String, CampoImportacion> aplicable = new LinkedHashMap<>();
    plantilla
        .mapeo()
        .forEach(
            (columna, campo) -> {
              if (columnas.contains(columna)) {
                aplicable.put(columna, campo);
              }
            });
    return conMapeo(
        columnas, aplicable, OrigenMapeo.PLANTILLA, plantilla.nombre(), faltantes, nuevas);
  }

  private ResultadoMapeo conMapeo(
      List<String> columnas,
      Map<String, CampoImportacion> mapeo,
      OrigenMapeo origen,
      String nombrePlantilla,
      List<String> columnasFaltantes,
      List<String> columnasNuevas) {
    List<String> errores = new ArrayList<>();
    Map<CampoImportacion, String> asignados = new EnumMap<>(CampoImportacion.class);
    Map<String, CampoImportacion> valido = new LinkedHashMap<>();
    mapeo.forEach(
        (columna, campo) -> {
          if (campo == null) {
            return;
          }
          if (!columnas.contains(columna)) {
            errores.add("La columna \"" + columna + "\" del mapeo no está en el archivo");
            return;
          }
          String anterior = asignados.putIfAbsent(campo, columna);
          if (anterior != null) {
            errores.add(
                "El campo "
                    + campo.getEtiqueta()
                    + " está asignado a dos columnas: \""
                    + anterior
                    + "\" y \""
                    + columna
                    + "\"");
            return;
          }
          valido.put(columna, campo);
        });
    return new ResultadoMapeo(
        valido,
        origen,
        nombrePlantilla,
        columnasFaltantes,
        columnasNuevas,
        obligatoriosSinMapear(valido),
        errores);
  }

  private boolean mismasColumnas(List<String> unas, List<String> otras) {
    return unas.size() == otras.size() && unas.containsAll(otras);
  }

  /** De dónde salió el mapeo aplicado. */
  public enum OrigenMapeo {
    MANUAL,
    PLANTILLA,
    AUTOMATICO
  }

  /** Plantilla guardada, ya leída de la base. */
  public record Plantilla(
      String nombre, List<String> columnas, Map<String, CampoImportacion> mapeo) {

    public Plantilla {
      Objects.requireNonNull(nombre);
      columnas = List.copyOf(columnas);
      mapeo = Map.copyOf(mapeo);
    }
  }

  /**
   * Mapeo resuelto.
   *
   * @param mapeo columna → campo, solo con asignaciones válidas
   * @param columnasFaltantes columnas de la plantilla que el archivo ya no trae
   * @param columnasNuevas columnas del archivo que la plantilla no conocía
   * @param errores problemas del mapeo en sí (columna inexistente, campo repetido)
   */
  public record ResultadoMapeo(
      Map<String, CampoImportacion> mapeo,
      OrigenMapeo origen,
      String nombrePlantilla,
      List<String> columnasFaltantes,
      List<String> columnasNuevas,
      List<CampoImportacion> obligatoriosSinMapear,
      List<String> errores) {}
}
