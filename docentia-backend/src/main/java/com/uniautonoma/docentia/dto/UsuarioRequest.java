package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.entity.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o editar un usuario. El vínculo depende del rol: el coordinador lleva
 * idFacultad, el docente idPrograma y el vicerrector ninguno de los dos.
 *
 * @param contrasena obligatoria al crear; al editar, si llega vacía se conserva la actual
 */
public record UsuarioRequest(
    @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombreCompleto,
    @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String correo,
    @NotNull(message = "El rol es obligatorio") Rol rol,
    Long idFacultad,
    Long idPrograma,
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String contrasena) {}
