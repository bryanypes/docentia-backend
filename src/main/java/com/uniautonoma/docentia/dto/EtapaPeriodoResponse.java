package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.EtapaPeriodo;
import com.uniautonoma.docentia.entity.TipoEtapa;
import java.time.LocalDate;

public record EtapaPeriodoResponse(
    TipoEtapa tipoEtapa, LocalDate fechaApertura, LocalDate fechaCierre) {

  public static EtapaPeriodoResponse desde(EtapaPeriodo etapa) {
    return new EtapaPeriodoResponse(
        etapa.getTipoEtapa(), etapa.getFechaApertura(), etapa.getFechaCierre());
  }
}
