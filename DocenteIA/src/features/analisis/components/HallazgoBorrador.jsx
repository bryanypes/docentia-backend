import { CircleX, History, MessageSquareQuote } from 'lucide-react'
import { useCallback, useState } from 'react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFechaHora } from '../../../shared/utils/formatters.js'
import { ESTADOS_HALLAZGO, ETIQUETA_VERSION } from '../estados.js'
import { listarVersionesHallazgo } from '../services/analisisService.js'

// Comentarios que se leen de entrada; los demás quedan plegados.
const CITAS_VISIBLES = 2

function Cita({ cita }) {
  return (
    <li className={`analisis__cita${cita.verificada ? '' : ' analisis__cita--no-verificada'}`}>
      <blockquote>«{cita.fragmento}»</blockquote>
      {!cita.verificada && (
        <p className="analisis__cita-estado">
          <CircleX size={14} aria-hidden="true" />
          No aparece tal cual en ningún comentario enviado: no cuenta como evidencia.
        </p>
      )}
      {cita.comentario && cita.comentario !== cita.fragmento && (
        <details>
          <summary>Ver el comentario completo</summary>
          <p className="analisis__comentario">{cita.comentario}</p>
        </details>
      )}
    </li>
  )
}

/** Quién lo incluyó, descartó o editó, y cuándo. Solo existe si alguien lo cambió. */
function Historial({ idHallazgo }) {
  const cargar = useCallback(() => listarVersionesHallazgo(idHallazgo), [idHallazgo])
  const { datos, error, cargando } = useFetch(cargar, [cargar])
  if (cargando) return <p className="campo__ayuda">Cargando el historial…</p>
  if (error) return <ErrorApi error={error} />
  return (
    <ol className="analisis__versiones">
      {datos.map((version) => (
        <li key={version.version}>
          <strong>{ETIQUETA_VERSION[version.accion] ?? version.accion}</strong>
          {' · '}
          {version.nombreUsuario ?? 'DocenteIA'} · {formatearFechaHora(version.fecha)}
          <p>{version.texto}</p>
          {version.motivo && <p>Motivo: {version.motivo}</p>}
        </li>
      ))}
    </ol>
  )
}

/**
 * Un hallazgo del borrador: agrupa los comentarios que dicen lo mismo dentro de un criterio de la
 * evaluación. Para quien revisa: incluir, descartar o editar.
 */
export function HallazgoBorrador({ hallazgo, puedeValidar, validando, error, onValidar, onDescartar, onEditar }) {
  const estado = ESTADOS_HALLAZGO[hallazgo.estado] ?? { etiqueta: hallazgo.estado, clase: 'neutro' }
  const sinCita = hallazgo.estado === 'SIN_CITA_VERIFICADA'
  const idAyuda = `ayuda-hallazgo-${hallazgo.idHallazgo}`
  const [modo, setModo] = useState(null)
  const [valor, setValor] = useState('')
  const [verHistorial, setVerHistorial] = useState(false)
  const citas = hallazgo.citas ?? []
  const visibles = citas.slice(0, CITAS_VISIBLES)
  const resto = citas.slice(CITAS_VISIBLES)
  const cambios = hallazgo.cambios ?? 0

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
        <div className="analisis__hallazgo-titulo">
          {hallazgo.nombreCriterio && (
            <p className="analisis__criterio" title="Criterio de la evaluación docente">
              {hallazgo.criterio} · {hallazgo.nombreCriterio}
            </p>
          )}
          <p className="analisis__hallazgo-texto">{hallazgo.texto}</p>
        </div>
        <span className={`pildora pildora--${estado.clase}`}>{estado.etiqueta}</span>
      </header>
      {citas.length === 0 ? (
        <p className="campo__ayuda">El análisis no aportó ningún comentario que lo respalde.</p>
      ) : (
        <>
          <p className="analisis__respaldo">
            <MessageSquareQuote size={15} aria-hidden="true" />
            {citas.length === 1
              ? 'Lo respalda 1 comentario de un estudiante'
              : `Lo respaldan ${citas.length} comentarios de estudiantes`}
          </p>
          <ul className="analisis__citas">
            {visibles.map((cita) => (
              <Cita key={cita.idCita} cita={cita} />
            ))}
          </ul>
          {resto.length > 0 && (
            <details className="analisis__mas-citas">
              <summary>
                Ver {resto.length === 1 ? 'el otro comentario' : `los otros ${resto.length} comentarios`}
              </summary>
              <ul className="analisis__citas">
                {resto.map((cita) => (
                  <Cita key={cita.idCita} cita={cita} />
                ))}
              </ul>
            </details>
          )}
        </>
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
      {cambios > 0 && (
        <details className="analisis__historial" onToggle={(e) => setVerHistorial(e.currentTarget.open)}>
          <summary>
            <History size={14} aria-hidden="true" /> Historial de cambios ({cambios})
          </summary>
          {verHistorial && <Historial key={hallazgo.estado + hallazgo.texto} idHallazgo={hallazgo.idHallazgo} />}
        </details>
      )}
    </article>
  )
}
