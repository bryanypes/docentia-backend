import { ArrowLeft, ClipboardCheck, ShieldCheck, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import robotSenala from '../../../assets/ilustraciones/robot-senala.svg'
import './PantallaAcceso.css'

const PUNTOS = [
  { icono: ShieldCheck, texto: 'Los comentarios de los estudiantes siempre son anónimos.' },
  { icono: ClipboardCheck, texto: 'Evaluación de la enseñanza según la Resolución 074.' },
  { icono: Sparkles, texto: 'Análisis de comentarios acompañado por DocenteIA.' },
]

/**
 * Pantalla de acceso (login y cambio de contraseña). En pantallas anchas muestra a la izquierda un
 * panel de marca con DocenteIA señalando el formulario; en celulares queda solo la tarjeta.
 *
 * @param avatar ilustración del robot; cambia según el estado del formulario
 * @param estadoAvatar texto breve para lectores de pantalla cuando el avatar comunica algo
 * @param tono 'error' sacude el avatar; 'espera' lo hace respirar mientras corre un contador
 * @param volverAlInicio muestra el enlace para regresar a la página de presentación
 */
export function PantallaAcceso({ avatar, estadoAvatar, tono = null, volverAlInicio = false, children }) {
  return (
    <div className="acceso">
      {volverAlInicio && (
        <Link to="/" className="acceso__volver">
          <ArrowLeft size={18} strokeWidth={2.25} aria-hidden="true" />
          Volver al inicio
        </Link>
      )}
      <section className="acceso__presentacion" aria-label="Sobre DOCENTIA">
        <p className="acceso__eyebrow">Evaluación Docente · Autónoma del Cauca</p>
        <h2 className="acceso__titular">
          Evaluar para <span>acompañar</span> el crecimiento de cada docente.
        </h2>
        <ul className="acceso__puntos">
          {PUNTOS.map(({ icono: Icono, texto }) => (
            <li key={texto}>
              <span className="acceso__punto-icono" aria-hidden="true">
                <Icono size={18} strokeWidth={2.25} />
              </span>
              {texto}
            </li>
          ))}
        </ul>
        <img className="acceso__robot-grande" src={robotSenala} alt="" />
      </section>

      <main className={`acceso__tarjeta${tono ? ` acceso__tarjeta--${tono}` : ''}`}>
        <div className="acceso__avatar">
          {/* key: al cambiar de ilustración se hace un fundido corto */}
          <img key={avatar} src={avatar} alt="" />
        </div>
        {estadoAvatar && (
          <span className="solo-lectores" role="status">
            {estadoAvatar}
          </span>
        )}

        <header className="acceso__marca">
          <p className="acceso__nombre">DOCENTIA</p>
          <img
            className="acceso__logo"
            src={`${import.meta.env.BASE_URL}brand/logo-uniautonoma.png`}
            alt="Universidad Autónoma del Cauca"
          />
          <p className="acceso__institucion">Evaluación docente institucional</p>
        </header>

        {children}
      </main>
    </div>
  )
}
