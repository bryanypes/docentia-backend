package com.uniautonoma.docentia.repository;

import com.uniautonoma.docentia.entity.Rol;
import com.uniautonoma.docentia.entity.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
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

  /**
   * Lista los usuarios activos de un rol, con su programa y facultad cargados.
   *
   * @param rol rol buscado
   * @return usuarios ordenados por nombre
   */
  @EntityGraph(attributePaths = {"programa", "programa.facultad"})
  List<Usuario> findByRolAndEsActivoTrueOrderByNombreCompletoAsc(Rol rol);

  /**
   * Lista los usuarios activos de un rol cuyos programas pertenecen a una facultad.
   *
   * @param rol rol buscado
   * @param idFacultad facultad a la que pertenece el programa del usuario
   * @return usuarios ordenados por nombre
   */
  @EntityGraph(attributePaths = {"programa", "programa.facultad"})
  List<Usuario> findByRolAndProgramaFacultadIdFacultadAndEsActivoTrueOrderByNombreCompletoAsc(
      Rol rol, Long idFacultad);

  /**
   * Busca un usuario activo por identificador y rol, con su programa y facultad cargados.
   *
   * @param idUsuario identificador del usuario
   * @param rol rol que debe tener
   * @return el usuario, o vacío si no existe, está inactivo o tiene otro rol
   */
  @EntityGraph(attributePaths = {"programa", "programa.facultad"})
  Optional<Usuario> findByIdUsuarioAndRolAndEsActivoTrue(Long idUsuario, Rol rol);
}
