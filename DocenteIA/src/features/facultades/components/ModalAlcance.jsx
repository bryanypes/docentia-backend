import { BookOpen, Building2 } from 'lucide-react'
import { useCallback } from 'react'
import { BotonAccion } from '../../../shared/components/ui/BotonAccion/BotonAccion.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Modal } from '../../../shared/components/ui/Modal/Modal.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { listarDocentes } from '../../docentes/services/docentesService.js'
import { listarProgramas } from '../../programas/services/programasService.js'
import './ModalAlcance.css'

/**
 * Información de una facultad o un programa: sus programas y sus docentes activos, cada uno con su
 * expediente a un clic.
 */
export function ModalAlcance({ facultad, programa, onCerrar }) {
  const idFacultad = facultad?.idFacultad ?? programa?.idFacultad
  const cargarDocentes = useCallback(() => listarDocentes(idFacultad), [idFacultad])
  const docentes = useFetch(cargarDocentes, [cargarDocentes])
  const cargarProgramas = useCallback(() => listarProgramas(idFacultad), [idFacultad])
  const programas = useFetch(cargarProgramas, [cargarProgramas])

  const delAlcance = (docentes.datos ?? []).filter((d) => !programa || d.idPrograma === programa.idPrograma)
  const listaProgramas = programa ? [] : (programas.datos ?? [])

  return (
    <Modal
      titulo={facultad?.nombre ?? programa.nombre}
      descripcion={programa ? `Programa de ${programa.nombreFacultad}` : 'Facultad'}
      icono={programa ? BookOpen : Building2}
      onCerrar={onCerrar}
    >
      {(docentes.cargando || programas.cargando) && <Cargando texto="Cargando la información" bloques={1} />}
      {docentes.error && (
        <EstadoVista tipo="error" titulo="No fue posible cargar los docentes" mensaje={docentes.error.message} />
      )}
      {docentes.datos && (
        <div className="alcance">
          <dl className="alcance__cifras">
            {!programa && (
              <div>
                <dt>Programas</dt>
                <dd>{listaProgramas.length}</dd>
              </div>
            )}
            <div>
              <dt>Docentes activos</dt>
              <dd>{delAlcance.length}</dd>
            </div>
          </dl>

          {!programa && listaProgramas.length > 0 && (
            <section>
              <h3 className="alcance__subtitulo">Programas</h3>
              <ul className="alcance__chips">
                {listaProgramas.map((p) => (
                  <li key={p.idPrograma}>
                    {p.nombre}
                    <span>{docentes.datos.filter((d) => d.idPrograma === p.idPrograma).length} docentes</span>
                  </li>
                ))}
              </ul>
            </section>
          )}

          <section>
            <h3 className="alcance__subtitulo">Docentes</h3>
            {delAlcance.length === 0 ? (
              <p className="campo__ayuda">Aún no hay docentes activos.</p>
            ) : (
              <ul className="alcance__docentes">
                {delAlcance.map((d) => (
                  <li key={d.idDocente}>
                    <span>
                      <strong>{d.nombreCompleto}</strong>
                      <span>
                        {d.correo}
                        {!programa && ` · ${d.nombrePrograma}`}
                      </span>
                    </span>
                    <BotonAccion tipo="ver" texto="Ver expediente" nombre={d.nombreCompleto} a={`/docentes/${d.idDocente}`} />
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      )}
    </Modal>
  )
}
