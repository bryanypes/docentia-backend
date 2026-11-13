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
 * Pantalla de acceso (login, recuperar y cambiar contraseña). Una tarjeta compacta con la marca en
 * una sola fila; en pantallas anchas, a la izquierda, un panel con DocenteIA y lo esencial.
 *
 * @param avatar ilustración del robot; cambia según el estado del formulario
 * @param estadoAvatar texto breve para lectores de pantalla cuando el avatar comunica algo
 * @param tono 'error' tiñe el aro del avatar de rojo; 'espera' lo hace latir en ámbar
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
        <div className="acceso__presentacion-cabecera">
          <span className="acceso__presentacion-robot" aria-hidden="true">
            <img src={robotSenala} alt="" />
          </span>
          <p className="acceso__eyebrow">Evaluación Docente · Autónoma del Cauca</p>
        </div>
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
      </section>

      <main className={`acceso__tarjeta${tono ? ` acceso__tarjeta--${tono}` : ''}`}>
        <header className="acceso__cabecera">
          <span className="acceso__avatar">
            {/* key: al cambiar de ilustración se hace un fundido corto */}
            <img key={avatar} src={avatar} alt="" />
          </span>
          <span className="acceso__marca">
            <strong>DOCENTIA</strong>
            <small>Evaluación Docente</small>
          </span>
          <img
            className="acceso__logo"
            src={`${import.meta.env.BASE_URL}brand/logo-uniautonoma.png`}
            alt="Universidad Autónoma del Cauca"
          />
        </header>
        {estadoAvatar && (
          <span className="solo-lectores" role="status">
            {estadoAvatar}
          </span>
        )}

        {children}
      </main>
    </div>
  )
}
