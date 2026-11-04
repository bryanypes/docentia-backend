package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Rol;
import java.time.LocalDateTime;

/** Datos propios que el usuario ve en Mi perfil, con nombres legibles de su facultad y programa. */
public record PerfilResponse(
    Long idUsuario,
    String nombreCompleto,
    String correo,
    String codigo,
    Rol rol,
    String nombreFacultad,
    String nombrePrograma,
    LocalDateTime fechaCreacion,
    boolean debeCambiarContrasena) {}
