package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.service.importacion.CampoImportacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * Plantilla a guardar. Lo normal es mandar las columnas y el mapeo que devolvió /validar.
 *
 * @param columnas encabezados del archivo con que se arma la plantilla
 * @param mapeo columna → campo; debe cubrir los campos obligatorios
 */
public record PlantillaMapeoRequest(
    @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,
    @NotEmpty(message = "Las columnas son obligatorias") List<String> columnas,
    @NotNull(message = "El mapeo es obligatorio") Map<String, CampoImportacion> mapeo) {}
