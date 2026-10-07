package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.TipoEtapa;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EtapaPeriodoRequest(
    @NotNull(message = "El tipo de etapa es obligatorio") TipoEtapa tipoEtapa,
    @NotNull(message = "La fecha de apertura es obligatoria") LocalDate fechaApertura,
    @NotNull(message = "La fecha de cierre es obligatoria") LocalDate fechaCierre) {}
