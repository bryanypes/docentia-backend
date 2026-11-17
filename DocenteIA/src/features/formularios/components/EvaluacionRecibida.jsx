import { CircleCheck, House } from 'lucide-react'
import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import robotAprobada from '../../../assets/ilustraciones/robot-aprobada.svg'
import { rutaInicioDelRol } from '../../../app/rutasPorRol.js'
import { useSesion } from '../../auth/sesionContext.js'
import './EvaluacionRecibida.css'

/**
 * Lo único que se ve después de enviar una evaluación: el avatar de IA confirma que se recibió, el
 * resumen por criterio y el camino de vuelta al inicio. Reemplaza al asistente de pasos, que ya no
 * tiene nada que hacer.
 *
 * @param detalle frase bajo el título (fecha de envío, anonimato…)
 * @param resumen [{ codigo, titulo, respondidas, total }] un renglón por criterio
 * @param accionSecundaria enlace opcional junto a "Volver al inicio"
 */
export function EvaluacionRecibida({ detalle, resumen, accionSecundaria }) {
  const { usuario } = useSesion()
  const titulo = useRef(null)
  const respondidas = resumen.reduce((suma, c) => suma + c.respondidas, 0)
  const total = resumen.reduce((suma, c) => suma + c.total, 0)

  // El contenido cambió por completo: el foco va al título para que el lector de pantalla lo anuncie.
  useEffect(() => {
    titulo.current?.focus()
  }, [])

  return (
    <section className="evaluacion-recibida" aria-labelledby="evaluacion-recibida-titulo">
      <div className="evaluacion-recibida__robot" aria-hidden="true">
        <img src={robotAprobada} alt="" />
      </div>

      <h2 id="evaluacion-recibida-titulo" ref={titulo} tabIndex={-1} className="evaluacion-recibida__titulo">
        Tu evaluación ha sido recibida
      </h2>
      {detalle && <p className="evaluacion-recibida__detalle">{detalle}</p>}

      <div className="evaluacion-recibida__resumen">
        <h3>
          Resumen
          <span>
            {respondidas} de {total} ítems respondidos
          </span>
        </h3>
        <ul>
          {resumen.map((criterio) => (
            <li key={criterio.codigo}>
              <CircleCheck size={17} aria-hidden="true" />
              <span className="evaluacion-recibida__criterio">{criterio.titulo}</span>
              <span className="evaluacion-recibida__conteo">
                {criterio.respondidas} de {criterio.total}
              </span>
            </li>
          ))}
        </ul>
      </div>

      <div className="evaluacion-recibida__acciones">
        <Link to={rutaInicioDelRol(usuario?.rol)} className="boton boton--primario">
          <House size={16} strokeWidth={2.25} aria-hidden="true" />
          Volver al inicio
        </Link>
        {accionSecundaria}
      </div>
    </section>
  )
}
