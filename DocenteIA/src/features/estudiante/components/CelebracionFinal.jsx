import { ArrowRight, HeartHandshake, ShieldCheck } from 'lucide-react'
import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import robotAprobada from '../../../assets/ilustraciones/robot-aprobada.svg'
import './CelebracionFinal.css'

// Piezas de confeti en los acentos de la marca; la posición y el retraso varían para que no caigan juntas.
const CONFETI = Array.from({ length: 18 }, (_, i) => ({
  tono: ['turquesa', 'violeta', 'coral', 'cielo', 'amarillo', 'rosa'][i % 6],
  izquierda: `${(i * 53) % 100}%`,
  retraso: `${(i % 6) * 0.12}s`,
  giro: `${(i * 47) % 360}deg`,
}))

/** Cierre cuando el estudiante envía su última evaluación del periodo. */
export function CelebracionFinal({ nombre, total }) {
  const titulo = useRef(null)
  useEffect(() => {
    titulo.current?.focus()
  }, [])

  return (
    <section className="celebracion" aria-labelledby="celebracion-titulo">
      <div className="celebracion__confeti" aria-hidden="true">
        {CONFETI.map((pieza, i) => (
          <span
            key={i}
            className={`celebracion__pieza celebracion__pieza--${pieza.tono}`}
            style={{ left: pieza.izquierda, animationDelay: pieza.retraso, '--giro': pieza.giro }}
          />
        ))}
      </div>
      <img className="celebracion__robot" src={robotAprobada} alt="" width="140" height="137" />
      <h2 id="celebracion-titulo" ref={titulo} tabIndex={-1} className="celebracion__titulo">
        ¡Terminaste, {nombre}!
      </h2>
      <p className="celebracion__texto">
        Evaluaste a {total === 1 ? 'tu profesor' : `tus ${total} profesores`} de este periodo.
      </p>
      <ul className="celebracion__notas">
        <li>
          <HeartHandshake size={18} aria-hidden="true" />
          Gracias: tus respuestas ayudan a mejorar la enseñanza en la universidad.
        </li>
        <li>
          <ShieldCheck size={18} aria-hidden="true" />
          Son anónimas: nadie puede ver qué respondiste.
        </li>
      </ul>
      <Link to="/evaluacion-profesores" className="boton boton--primario celebracion__accion">
        Ver mis evaluaciones
        <ArrowRight size={16} aria-hidden="true" />
      </Link>
    </section>
  )
}
