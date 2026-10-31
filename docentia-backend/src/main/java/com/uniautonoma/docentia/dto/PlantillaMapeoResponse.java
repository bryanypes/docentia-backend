package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.service.importacion.CampoImportacion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PlantillaMapeoResponse(
    Long idPlantilla,
    String nombre,
    List<String> columnas,
    Map<String, CampoImportacion> mapeo,
    LocalDateTime fechaActualizacion) {}
