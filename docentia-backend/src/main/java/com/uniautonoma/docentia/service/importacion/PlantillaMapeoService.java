package com.uniautonoma.docentia.service.importacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniautonoma.docentia.dto.PlantillaMapeoRequest;
import com.uniautonoma.docentia.dto.PlantillaMapeoResponse;
import com.uniautonoma.docentia.entity.PlantillaMapeo;
import com.uniautonoma.docentia.exception.DatosInvalidosException;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.PlantillaMapeoRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.importacion.ResolutorMapeo.Plantilla;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Plantillas de mapeo de columnas, una lista propia por usuario. */
@Service
@Transactional
public class PlantillaMapeoService {

  private static final TypeReference<List<String>> TIPO_COLUMNAS = new TypeReference<>() {};
  private static final TypeReference<Map<String, CampoImportacion>> TIPO_MAPEO =
      new TypeReference<>() {};

  private final PlantillaMapeoRepository plantillaRepository;
  private final UsuarioRepository usuarioRepository;
  private final ObjectMapper objectMapper;

  public PlantillaMapeoService(
      PlantillaMapeoRepository plantillaRepository,
      UsuarioRepository usuarioRepository,
      ObjectMapper objectMapper) {
    this.plantillaRepository = plantillaRepository;
    this.usuarioRepository = usuarioRepository;
    this.objectMapper = objectMapper;
  }

  /**
   * Lista las plantillas del usuario.
   *
   * @return de la más reciente a la más antigua
   */
  @Transactional(readOnly = true)
  public List<PlantillaMapeoResponse> listar(UsuarioSesion sesion) {
    return plantillaRepository
        .findByUsuarioIdUsuarioOrderByFechaActualizacionDesc(sesion.idUsuario())
        .stream()
        .map(this::aRespuesta)
        .toList();
  }

  /**
   * Guarda una plantilla. Si el usuario ya tiene una con ese nombre, la reemplaza.
   *
   * @throws DatosInvalidosException si el mapeo usa columnas que no están en la lista, asigna un
   *     campo dos veces o no cubre los obligatorios
   */
  public PlantillaMapeoResponse guardar(PlantillaMapeoRequest solicitud, UsuarioSesion sesion) {
    String nombre = solicitud.nombre().trim();
    ResolutorMapeo.ResultadoMapeo revisado =
        new ResolutorMapeo()
            .resolver(solicitud.columnas(), solicitud.mapeo(), Optional.empty(), List.of());
    if (!revisado.errores().isEmpty()) {
      throw new DatosInvalidosException(revisado.errores().get(0));
    }
    if (!revisado.obligatoriosSinMapear().isEmpty()) {
      throw new DatosInvalidosException(
          "La plantilla debe asignar los campos obligatorios: "
              + revisado.obligatoriosSinMapear().stream()
                  .map(CampoImportacion::getEtiqueta)
                  .collect(Collectors.joining(", ")));
    }
    PlantillaMapeo plantilla =
        plantillaRepository
            .findByUsuarioIdUsuarioAndNombreIgnoreCase(sesion.idUsuario(), nombre)
            .orElseGet(
                () ->
                    new PlantillaMapeo(
                        usuarioRepository.getReferenceById(sesion.idUsuario()), nombre));
    plantilla.reemplazar(aJson(solicitud.columnas()), aJson(revisado.mapeo()));
    return aRespuesta(plantillaRepository.saveAndFlush(plantilla));
  }

  /**
   * Borra una plantilla propia.
   *
   * @throws RecursoNoEncontradoException si no existe o es de otro usuario
   */
  public void eliminar(Long idPlantilla, UsuarioSesion sesion) {
    plantillaRepository.delete(
        plantillaRepository
            .findByIdPlantillaAndUsuarioIdUsuario(idPlantilla, sesion.idUsuario())
            .orElseThrow(() -> new RecursoNoEncontradoException("Plantilla no encontrada")));
  }

  /**
   * Busca una plantilla propia por nombre para aplicarla.
   *
   * @throws RecursoNoEncontradoException si el usuario no tiene una plantilla con ese nombre
   */
  @Transactional(readOnly = true)
  public Plantilla buscarPorNombre(String nombre, UsuarioSesion sesion) {
    return plantillaRepository
        .findByUsuarioIdUsuarioAndNombreIgnoreCase(sesion.idUsuario(), nombre.trim())
        .map(this::aPlantilla)
        .orElseThrow(() -> new RecursoNoEncontradoException("Plantilla no encontrada"));
  }

  /** Todas las plantillas del usuario, para reconocer una con las mismas columnas del archivo. */
  @Transactional(readOnly = true)
  public List<Plantilla> plantillasDe(UsuarioSesion sesion) {
    return plantillaRepository
        .findByUsuarioIdUsuarioOrderByFechaActualizacionDesc(sesion.idUsuario())
        .stream()
        .map(this::aPlantilla)
        .toList();
  }

  /**
   * Lee el mapeo que llega como texto JSON en el formulario.
   *
   * @return el mapeo, o nulo si no se envió
   * @throws DatosInvalidosException si no es un objeto columna → campo con campos conocidos
   */
  public Map<String, CampoImportacion> leerMapeo(String mapeoJson) {
    if (!StringUtils.hasText(mapeoJson)) {
      return null;
    }
    try {
      Map<String, CampoImportacion> mapeo = objectMapper.readValue(mapeoJson, TIPO_MAPEO);
      return mapeo == null ? Map.of() : new LinkedHashMap<>(mapeo);
    } catch (JsonProcessingException excepcion) {
      throw new DatosInvalidosException(
          "El mapeo debe ser un objeto JSON de columna a campo, con campos de la lista esperada");
    }
  }

  private Plantilla aPlantilla(PlantillaMapeo plantilla) {
    return new Plantilla(
        plantilla.getNombre(),
        leer(plantilla.getColumnas(), TIPO_COLUMNAS),
        leer(plantilla.getMapeo(), TIPO_MAPEO));
  }

  private PlantillaMapeoResponse aRespuesta(PlantillaMapeo plantilla) {
    return new PlantillaMapeoResponse(
        plantilla.getIdPlantilla(),
        plantilla.getNombre(),
        leer(plantilla.getColumnas(), TIPO_COLUMNAS),
        leer(plantilla.getMapeo(), TIPO_MAPEO),
        plantilla.getFechaActualizacion());
  }

  private String aJson(Object valor) {
    try {
      return objectMapper.writeValueAsString(valor);
    } catch (JsonProcessingException excepcion) {
      throw new IllegalStateException("No se pudo convertir la plantilla a JSON", excepcion);
    }
  }

  private <T> T leer(String json, TypeReference<T> tipo) {
    try {
      return objectMapper.readValue(json, tipo);
    } catch (JsonProcessingException excepcion) {
      throw new IllegalStateException("La plantilla guardada no es un JSON válido", excepcion);
    }
  }
}
