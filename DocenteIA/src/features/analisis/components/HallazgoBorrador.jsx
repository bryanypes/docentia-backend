import { CircleCheck, CircleX } from 'lucide-react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { ESTADOS_HALLAZGO } from '../estados.js'

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

/** Un hallazgo del borrador con sus citas y, para la coordinación, la acción de validarlo. */
export function HallazgoBorrador({ hallazgo, puedeValidar, validando, error, onValidar }) {
  const estado = ESTADOS_HALLAZGO[hallazgo.estado] ?? { etiqueta: hallazgo.estado, clase: 'neutro' }
  const sinCita = hallazgo.estado === 'SIN_CITA_VERIFICADA'
  const idAyuda = `ayuda-hallazgo-${hallazgo.idHallazgo}`

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
      {puedeValidar && hallazgo.estado !== 'VALIDADO' && (
        <div className="analisis__acciones">
          <Boton
            variante="contorno"
            disabled={validando}
            aria-describedby={sinCita ? idAyuda : undefined}
            onClick={() => onValidar(hallazgo)}
          >
            {validando ? 'Validando…' : 'Marcar como válido'}
          </Boton>
          {sinCita && (
            <span id={idAyuda} className="campo__ayuda">
              Sin una cita verificada el sistema no permite validarlo.
            </span>
          )}
        </div>
      )}
    </article>
  )
}
