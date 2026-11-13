import { ArrowRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import robotSaludo from '../../assets/ilustraciones/robot-saludo.svg'

/** Cabecera de las páginas públicas (presentación y tutoriales). */
export function CabeceraPublica({ enlaces = [] }) {
  return (
    <header className="presentacion__cabecera">
      <Link to="/" className="presentacion__marca" aria-label="DOCENTIA, inicio">
        <span className="presentacion__marca-icono" aria-hidden="true">
          <img src={robotSaludo} alt="" />
        </span>
        <span>
          <strong>DOCENTIA</strong>
          <small>Evaluación Docente</small>
        </span>
      </Link>
      <img
        className="presentacion__logo-cabecera"
        src={`${import.meta.env.BASE_URL}brand/logo-uniautonoma.png`}
        alt="Universidad Autónoma del Cauca"
      />
      {enlaces.length > 0 && (
        <nav className="presentacion__nav" aria-label="Secciones">
          {enlaces.map(([texto, destino]) =>
            destino.startsWith('#') ? (
              <a key={texto} href={destino}>
                {texto}
              </a>
            ) : (
              <Link key={texto} to={destino}>
                {texto}
              </Link>
            ),
          )}
        </nav>
      )}
      <Link to="/login" className="presentacion__boton presentacion__boton--claro">
        Iniciar sesión <ArrowRight size={17} aria-hidden="true" />
      </Link>
    </header>
  )
}
