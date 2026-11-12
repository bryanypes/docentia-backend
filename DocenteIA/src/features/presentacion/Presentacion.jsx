import { Link } from 'react-router-dom'
import './Presentacion.css'

const PASOS = [
  {
    titulo: 'Se recogen las tres evaluaciones',
    texto:
      'Los estudiantes evalúan a sus profesores de forma anónima, la coordinación hace la coevaluación y cada docente su autoevaluación, con los instrumentos del Anexo 1.',
  },
  {
    titulo: 'Se calcula y se analiza',
    texto:
      'La nota se pondera 50 % estudiantes, 30 % coordinación y 20 % autoevaluación, se clasifica en cinco niveles y los comentarios se resumen en fortalezas y oportunidades con citas textuales.',
  },
  {
    titulo: 'Se cierra con el docente',
    texto:
      'En una reunión de retroalimentación, coordinación y docente firman el acta; bajo 3,80 el acta incluye compromisos con plazo que se siguen el periodo siguiente.',
  },
]

const PUBLICO = [
  ['Estudiantes', 'Evalúan a los profesores de sus clases, sin que nadie pueda saber qué respondió cada uno.'],
  ['Docentes', 'Responden su autoevaluación, reservan su cita y ven su resultado y su acta en la reunión.'],
  ['Coordinación', 'Coevalúa, revisa el análisis de comentarios, agenda las reuniones y firma las actas.'],
  ['Vicerrectoría', 'Define periodos y fechas, ve los indicadores de toda la universidad y corrige actas con motivo.'],
]

/** Presentación pública de DOCENTIA, sin sesión. */
export default function Presentacion() {
  return (
    <div className="presentacion">
      <header className="presentacion__cabecera">
        <span className="presentacion__marca">DOCENTIA</span>
        <Link to="/login" className="presentacion__boton presentacion__boton--claro">
          Iniciar sesión
        </Link>
      </header>

      <main>
        <section className="presentacion__heroe">
          <p className="presentacion__antetitulo">Corporación Universitaria Autónoma del Cauca</p>
          <h1>Evaluación docente de punta a punta</h1>
          <p>
            DOCENTIA lleva el proceso de evaluación docente que define la Resolución 074 de 2026: desde que
            estudiantes, coordinación y docente responden sus instrumentos hasta que el docente firma su acta y se
            hace seguimiento a sus compromisos.
          </p>
          <Link to="/login" className="presentacion__boton">
            Iniciar sesión
          </Link>
        </section>

        <section className="presentacion__seccion" aria-labelledby="titulo-pasos">
          <h2 id="titulo-pasos">Cómo funciona</h2>
          <ol className="presentacion__pasos">
            {PASOS.map((paso, i) => (
              <li key={paso.titulo}>
                <span className="presentacion__numero" aria-hidden="true">
                  {i + 1}
                </span>
                <h3>{paso.titulo}</h3>
                <p>{paso.texto}</p>
              </li>
            ))}
          </ol>
        </section>

        <section className="presentacion__seccion" aria-labelledby="titulo-publico">
          <h2 id="titulo-publico">Para quién es</h2>
          <ul className="presentacion__publico">
            {PUBLICO.map(([rol, texto]) => (
              <li key={rol}>
                <h3>{rol}</h3>
                <p>{texto}</p>
              </li>
            ))}
          </ul>
        </section>
      </main>

      <footer className="presentacion__pie">DOCENTIA · Uniautónoma del Cauca · Resolución 074 de 2026</footer>
    </div>
  )
}
