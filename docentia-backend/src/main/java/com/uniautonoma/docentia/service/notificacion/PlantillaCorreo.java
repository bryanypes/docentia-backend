package com.uniautonoma.docentia.service.notificacion;

import com.uniautonoma.docentia.entity.Usuario;

/** Textos del correo: texto plano, sin imágenes y con solo el aviso genérico y el enlace. */
final class PlantillaCorreo {

  private static final String INSTITUCION = "Corporación Universitaria Autónoma del Cauca";

  private final String urlAplicacion;

  PlantillaCorreo(String urlAplicacion) {
    this.urlAplicacion = urlAplicacion.replaceAll("/+$", "");
  }

  String mensaje(EventoNotificacion evento) {
    String base = evento.tipo().getMensaje();
    return evento.detalle() == null ? base : base + " " + evento.detalle();
  }

  String asunto(EventoNotificacion evento) {
    return "DOCENTIA: " + evento.tipo().getTitulo();
  }

  String cuerpo(Usuario usuario, String mensaje, String enlace) {
    return String.join(
        "\n",
        "Hola, " + usuario.getNombreCompleto() + ".",
        "",
        mensaje,
        "",
        "Ingresa a DOCENTIA: " + urlAplicacion + (enlace == null ? "" : enlace),
        "",
        INSTITUCION,
        "Este es un aviso automático; no respondas este correo.");
  }
}
