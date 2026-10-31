import { CircleCheck, CircleX } from 'lucide-react'
import { useCallback, useState } from 'react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFechaHora } from '../../../shared/utils/formatters.js'
import { ESTADOS_HALLAZGO, ETIQUETA_VERSION } from '../estados.js'
import { listarVersionesHallazgo } from '../services/analisisService.js'

function Cita({ cita }) {
  const Icono = cita.verificada ? CircleCheck : CircleX
  return (
    <li className={`analisis__cita${cita.verificada ? '' : ' analisis__cita--no-verificada'}`}>
      <blockquote>«{cita.fragmento}»</blockquote>
      <p className="analisis__cita-estado">
        <Icono size={14} aria-hidden="true" />
        {cita.verificada
          ? 'Aparece en el comentario de un estudiante.'
          : 'No aparece tal cual en ningún comentario enviado: no cuenta como evidencia.'}
      </p>
      {cita.comentario && (
        <details>
          <summary>Ver el comentario de origen</summary>
          <p className="analisis__comentario">{cita.comentario}</p>
        </details>
      )}
    </li>
  )
}

/** Historial del hallazgo: cada versión con quién la hizo, el modelo y la instrucción. */
function Versiones({ idHallazgo }) {
  const cargar = useCallback(() => listarVersionesHallazgo(idHallazgo), [idHallazgo])
  const { datos, error, cargando } = useFetch(cargar, [cargar])
  if (cargando) return <p className="campo__ayuda">Cargando versiones…</p>
  if (error) return <ErrorApi error={error} />
  if (datos.length === 0) return <p className="campo__ayuda">Todavía no tiene cambios.</p>
  return (
    <ol className="analisis__versiones">
      {datos.map((version) => (
        <li key={version.version}>
          <strong>
            v{version.version} · {ETIQUETA_VERSION[version.accion] ?? version.accion}
          </strong>{' '}
          {version.nombreUsuario ?? version.modelo} · {formatearFechaHora(version.fecha)} · {version.modelo} (
          {version.versionInstruccion})<p>{version.texto}</p>
          {version.motivo && <p>Motivo: {version.motivo}</p>}
        </li>
      ))}
    </ol>
  )
}

/** Un hallazgo del borrador con sus citas y, para la coordinación, incluir, descartar o editar. */
export function HallazgoBorrador({ hallazgo, puedeValidar, validando, error, onValidar, onDescartar, onEditar }) {
  const estado = ESTADOS_HALLAZGO[hallazgo.estado] ?? { etiqueta: hallazgo.estado, clase: 'neutro' }
  const sinCita = hallazgo.estado === 'SIN_CITA_VERIFICADA'
  const idAyuda = `ayuda-hallazgo-${hallazgo.idHallazgo}`
  const [modo, setModo] = useState(null)
  const [valor, setValor] = useState('')
  const [verVersiones, setVerVersiones] = useState(false)

  function abrir(nuevoModo) {
    setModo(modo === nuevoModo ? null : nuevoModo)
    setValor(nuevoModo === 'editar' ? hallazgo.texto : '')
  }

  async function confirmar(evento) {
    evento.preventDefault()
    const hecho = modo === 'editar' ? await onEditar(hallazgo, valor.trim()) : await onDescartar(hallazgo, valor.trim())
    if (hecho) setModo(null)
  }

  return (
    <article className="analisis__hallazgo">
      <header className="analisis__hallazgo-cabecera">
        <p className="analisis__hallazgo-texto">{hallazgo.texto}</p>
        <span className={`pildora pildora--${estado.clase}`}>{estado.etiqueta}</span>
      </header>
      {hallazgo.citas.length === 0 ? (
        <p className="campo__ayuda">El análisis no aportó ninguna cita para este hallazgo.</p>
      ) : (
        <ul className="analisis__citas">
          {hallazgo.citas.map((cita) => (
            <Cita key={cita.idCita} cita={cita} />
          ))}
        </ul>
      )}
      <ErrorApi error={error} />
      {puedeValidar && (
        <div className="analisis__acciones">
          {hallazgo.estado !== 'VALIDADO' && (
            <Boton
              variante="contorno"
              disabled={validando}
              aria-describedby={sinCita ? idAyuda : undefined}
              onClick={() => onValidar(hallazgo)}
            >
              {validando ? 'Validando…' : 'Marcar como válido'}
            </Boton>
          )}
          {hallazgo.estado !== 'DESCARTADO' && (
            <Boton variante="contorno" disabled={validando} onClick={() => abrir('descartar')}>
              Descartar
            </Boton>
          )}
          <Boton variante="contorno" disabled={validando} onClick={() => abrir('editar')}>
            Editar texto
          </Boton>
          {sinCita && (
            <span id={idAyuda} className="campo__ayuda">
              Sin una cita verificada el sistema no permite validarlo.
            </span>
          )}
        </div>
      )}
      {modo && (
        <form className="analisis__edicion" onSubmit={confirmar}>
          <Campo
            id={`${modo}-${hallazgo.idHallazgo}`}
            etiqueta={modo === 'editar' ? 'Texto del hallazgo' : 'Motivo del descarte (opcional)'}
          >
            <textarea
              id={`${modo}-${hallazgo.idHallazgo}`}
              className="campo__control"
              rows={3}
              maxLength={modo === 'editar' ? 1000 : 500}
              required={modo === 'editar'}
              value={valor}
              onChange={(e) => setValor(e.target.value)}
            />
          </Campo>
          <Boton type="submit" disabled={validando}>
            {modo === 'editar' ? 'Guardar texto' : 'Confirmar descarte'}
          </Boton>
        </form>
      )}
      <details onToggle={(e) => setVerVersiones(e.currentTarget.open)}>
        <summary>Ver versiones</summary>
        {verVersiones && <Versiones key={hallazgo.estado + hallazgo.texto} idHallazgo={hallazgo.idHallazgo} />}
      </details>
    </article>
  )
}
