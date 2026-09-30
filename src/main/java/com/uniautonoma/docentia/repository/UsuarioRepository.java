package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de los usuarios del sistema. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

  /**
   * Busca un usuario activo por su correo institucional, sin distinguir mayúsculas.
   *
   * @param correo correo con el que el usuario ingresa
   * @return el usuario, o vacío si no existe o está inactivo
   */
  Optional<Usuario> findByCorreoIgnoreCaseAndEsActivoTrue(String correo);
}
