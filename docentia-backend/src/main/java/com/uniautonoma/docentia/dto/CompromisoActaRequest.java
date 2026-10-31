package com.uniautonoma.docentia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Compromiso de mejora con su plazo. */
public record CompromisoActaRequest(
    @NotBlank(message = "Describe el compromiso")
        @Size(max = 500, message = "El compromiso no puede superar los 500 caracteres")
        String descripcion,
    @NotNull(message = "El plazo es obligatorio") LocalDate fechaLimite) {}
