import { RotateCcw, Search } from 'lucide-react'
import { useCallback, useMemo, useState } from 'react'
import { Aviso } from '../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando } from '../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../shared/components/ui/Panel/Panel.jsx'
import { ROLES } from '../../shared/constants/dominio.js'
import { useFetch } from '../../shared/hooks/useFetch.js'
import { formatearFechaHora, normalizarTexto } from '../../shared/utils/formatters.js'
import { listarUsuarios } from '../usuarios/services/usuariosService.js'
import { listarFormulariosDocente, reabrirDirecto } from './correccionesService.js'
import { ETIQUETA_TIPO } from './estados.js'

const MOTIVO_MINIMO = 10
const EVALUABLES = [ROLES.DOCENTE, ROLES.COORDINADOR]

/**
 * La administración reabre una autoevaluación o coevaluación ya enviada sin que nadie lo pida ni
 * lo verifique el superior, para que quien la respondió la vuelva a hacer. Queda en la auditoría.
 */
export function ReabrirDirecto({ onReabierto }) {
  const usuarios = useFetch(() => listarUsuarios())
  const [busqueda, setBusqueda] = useState('')
  const [idDocente, setIdDocente] = useState('')

  const docentes = useMemo(() => {
    const termino = normalizarTexto(busqueda.trim())
    return (usuarios.datos ?? [])
      .filter((u) => EVALUABLES.includes(u.rol) && u.esActivo !== false)
      .filter((u) => !termino || normalizarTexto(`${u.nombreCompleto} ${u.correo}`).includes(termino))
  }, [usuarios.datos, busqueda])

  return (
    <Panel
      titulo="Reabrir una evaluación"
      descripcion="Reabre una autoevaluación o coevaluación ya enviada para que se vuelva a responder, sin solicitud ni verificación del superior. Queda registrada en la auditoría."
      icono={RotateCcw}
      tono="coral"
    >
      <div className="reabrir__busqueda">
        <label className="reabrir__buscador">
          <Search size={16} aria-hidden="true" />
          <span className="solo-lectores">Buscar docente</span>
          <input
            type="search"
            className="campo__control"
            placeholder="Buscar docente por nombre o correo"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </label>
        <Campo id="docenteReabrir" etiqueta="Docente">
          <select
            id="docenteReabrir"
            className="campo__control"
            value={idDocente}
            onChange={(e) => setIdDocente(e.target.value)}
          >
            <option value="">{usuarios.cargando ? 'Cargando…' : `Elige uno (${docentes.length})`}</option>
            {docentes.map((u) => (
              <option key={u.idUsuario} value={u.idUsuario}>
                {u.nombreCompleto}
                {u.rol === ROLES.COORDINADOR ? ' · coordinación' : ''}
              </option>
            ))}
          </select>
        </Campo>
      </div>
      <ErrorApi error={usuarios.error} />
      {idDocente && <FormulariosDelDocente key={idDocente} idDocente={idDocente} onReabierto={onReabierto} />}
    </Panel>
  )
}

function FormulariosDelDocente({ idDocente, onReabierto }) {
  const cargar = useCallback(() => listarFormulariosDocente(idDocente), [idDocente])
  const formularios = useFetch(cargar, [cargar])
  const [hecho, setHecho] = useState(null)

  if (formularios.cargando && !formularios.datos) return <Cargando texto="Buscando sus evaluaciones" bloques={1} />
  if (formularios.error) return <ErrorApi error={formularios.error} />
  const lista = formularios.datos ?? []

  return (
    <div className="reabrir__lista">
      {hecho && (
        <Aviso tipo="exito" titulo="Evaluación reabierta">
          Le avisamos a {hecho} para que la vuelva a responder.
        </Aviso>
      )}
      {lista.length === 0 && (
        <p className="campo__ayuda">Este docente no tiene autoevaluación ni coevaluación en el periodo activo.</p>
      )}
      {lista.map((f) => (
        <Formulario
          key={f.idFormulario}
          formulario={f}
          onReabierto={() => {
            setHecho(f.nombreEvaluador)
            formularios.recargar()
            onReabierto?.()
          }}
        />
      ))}
    </div>
  )
}

function Formulario({ formulario, onReabierto }) {
  const [abierto, setAbierto] = useState(false)
  const [motivo, setMotivo] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState(null)
  const id = `motivo-reabrir-${formulario.idFormulario}`

  async function reabrir(evento) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      await reabrirDirecto(formulario.idFormulario, motivo.trim())
      setAbierto(false)
      setMotivo('')
      onReabierto()
    } catch (errorApi) {
      setError(errorApi)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <article className="reabrir__formulario">
      <div className="reabrir__datos">
        <strong>{ETIQUETA_TIPO[formulario.tipo] ?? formulario.tipo}</strong>
        <span>
          La responde {formulario.nombreEvaluador}
          {formulario.enviadaEn ? ` · enviada el ${formatearFechaHora(formulario.enviadaEn)}` : ''}
        </span>
        {!formulario.reabrible && <span className="reabrir__motivo-no">{formulario.motivoNoReabrible}</span>}
      </div>
      {formulario.reabrible && !abierto && (
        <Boton variante="contorno" icono={RotateCcw} onClick={() => setAbierto(true)}>
          Reabrir
        </Boton>
      )}
      {abierto && (
        <form className="reabrir__confirmar" onSubmit={reabrir}>
          <Campo id={id} etiqueta="Motivo de la reapertura" ayuda={`Al menos ${MOTIVO_MINIMO} caracteres. Queda en la auditoría.`}>
            <textarea
              id={id}
              className="campo__control"
              rows={2}
              maxLength={500}
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
            />
          </Campo>
          <ErrorApi error={error} />
          <div className="reabrir__botones">
            <Boton type="submit" disabled={enviando || motivo.trim().length < MOTIVO_MINIMO}>
              {enviando ? 'Reabriendo…' : 'Confirmar reapertura'}
            </Boton>
            <Boton variante="enlace" onClick={() => setAbierto(false)}>
              Cancelar
            </Boton>
          </div>
        </form>
      )}
    </article>
  )
}
