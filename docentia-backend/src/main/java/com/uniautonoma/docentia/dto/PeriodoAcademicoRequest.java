package com.uniautonoma.docentia.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record PeriodoAcademicoRequest(
    @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 20, message = "El nombre no puede superar los 20 caracteres")
        String nombre,
    @NotNull(message = "La fecha de inicio es obligatoria") LocalDate fechaInicio,
    @NotNull(message = "La fecha de fin es obligatoria") LocalDate fechaFin,
    @NotNull(message = "Las etapas son obligatorias") List<@Valid EtapaPeriodoRequest> etapas) {}
