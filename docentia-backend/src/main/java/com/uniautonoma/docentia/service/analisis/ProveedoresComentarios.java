package com.uniautonoma.docentia.service.analisis;

import com.uniautonoma.docentia.entity.FuenteComentarios;
import org.springframework.stereotype.Component;

/** Elige el proveedor de comentarios según la fuente de la solicitud. */
@Component
public class ProveedoresComentarios {

  private final ComentariosImportadosProveedor csv;
  private final ComentariosFormularioProveedor formulario;

  public ProveedoresComentarios(
      ComentariosImportadosProveedor csv, ComentariosFormularioProveedor formulario) {
    this.csv = csv;
    this.formulario = formulario;
  }

  public ProveedorComentarios para(FuenteComentarios fuente) {
    return fuente == FuenteComentarios.FORMULARIO ? formulario : csv;
  }
}
