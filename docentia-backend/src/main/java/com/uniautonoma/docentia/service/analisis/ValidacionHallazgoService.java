package com.uniautonoma.docentia.service.analisis;

import com.uniautonoma.docentia.dto.SolicitudAnalisisResponse.HallazgoBorrador;
import com.uniautonoma.docentia.entity.Hallazgo;
import com.uniautonoma.docentia.entity.TipoEtapa;
import com.uniautonoma.docentia.exception.EtapaCerradaException;
import com.uniautonoma.docentia.exception.NoProcesableException;
import com.uniautonoma.docentia.exception.RecursoNoEncontradoException;
import com.uniautonoma.docentia.repository.HallazgoRepository;
import com.uniautonoma.docentia.repository.UsuarioRepository;
import com.uniautonoma.docentia.security.UsuarioSesion;
import com.uniautonoma.docentia.service.DocenteService;
import com.uniautonoma.docentia.service.VentanaEtapaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validación de hallazgos uno a uno. Solo se valida lo que tiene respaldo: un hallazgo sin ninguna
 * cita verificada se rechaza. La revisión completa del borrador (editar, descartar, publicar al
 * docente) es la historia E4.
 */
@Service
@Transactional
public class ValidacionHallazgoService {

  private final HallazgoRepository hallazgoRepository;
  private final UsuarioRepository usuarioRepository;
  private final DocenteService docenteService;
  private final VentanaEtapaService ventanaEtapaService;

  public ValidacionHallazgoService(
      HallazgoRepository hallazgoRepository,
      UsuarioRepository usuarioRepository,
      DocenteService docenteService,
      VentanaEtapaService ventanaEtapaService) {
    this.hallazgoRepository = hallazgoRepository;
    this.usuarioRepository = usuarioRepository;
    this.docenteService = docenteService;
    this.ventanaEtapaService = ventanaEtapaService;
  }

  /**
   * Marca un hallazgo como válido.
   *
   * @throws RecursoNoEncontradoException si no existe o su docente está fuera del alcance
   * @throws EtapaCerradaException si la revisión de hallazgos del periodo está cerrada
   * @throws NoProcesableException si el hallazgo no tiene ninguna cita verificada
   */
  public HallazgoBorrador validar(Long idHallazgo, UsuarioSesion sesion) {
    Hallazgo hallazgo =
        hallazgoRepository
            .findById(idHallazgo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Hallazgo no encontrado"));
    try {
      docenteService.buscarDocenteVisible(
          hallazgo.getSolicitud().getDocente().getIdUsuario(), sesion);
    } catch (RecursoNoEncontradoException fueraDeAlcance) {
      throw new RecursoNoEncontradoException("Hallazgo no encontrado");
    }
    ventanaEtapaService.exigirEtapaAbierta(
        hallazgo.getSolicitud().getPeriodo(), TipoEtapa.REVISION_HALLAZGOS);
    if (!hallazgo.tieneCitaVerificada()) {
      throw new NoProcesableException(
          "El hallazgo no tiene ninguna cita que aparezca en los comentarios de los estudiantes;"
              + " no se puede marcar como válido");
    }
    hallazgo.validar(usuarioRepository.getReferenceById(sesion.idUsuario()));
    return HallazgoBorrador.desde(hallazgoRepository.saveAndFlush(hallazgo));
  }
}
