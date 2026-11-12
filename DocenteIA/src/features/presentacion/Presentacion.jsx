import { ArrowDown, ArrowRight, ChartColumn, ClipboardCheck, FileSignature, GraduationCap, UsersRound } from 'lucide-react'
import { Link } from 'react-router-dom'
import './Presentacion.css'

const PASOS = [
  {
    Icono: ClipboardCheck,
    titulo: 'Se recogen las tres evaluaciones',
    texto:
      'Los estudiantes evalúan a sus profesores de forma anónima, la coordinación hace la coevaluación y cada docente su autoevaluación, con los instrumentos del Anexo 1.',
  },
  {
    Icono: ChartColumn,
    titulo: 'Se calcula y se analiza',
    texto:
      'La nota se pondera 50 % estudiantes, 30 % coordinación y 20 % autoevaluación, se clasifica en cinco niveles y los comentarios se resumen en fortalezas y oportunidades con citas textuales.',
  },
  {
    Icono: FileSignature,
    titulo: 'Se cierra con el docente',
    texto:
      'En una reunión de retroalimentación, coordinación y docente firman el acta; bajo 3,80 el acta incluye compromisos con plazo que se siguen el periodo siguiente.',
  },
]

const PUBLICO = [
  [UsersRound, 'Estudiantes', 'Evalúan a sus profesores sin que se identifique qué respondió cada persona.'],
  [GraduationCap, 'Docentes', 'Responden su autoevaluación, reservan su cita y revisan su resultado y acta.'],
  [ClipboardCheck, 'Coordinación', 'Coevalúa, revisa comentarios, agenda reuniones y firma las actas.'],
  [ChartColumn, 'Vicerrectoría', 'Define periodos, consulta indicadores y corrige actas con motivo.'],
]

/** Presentación pública de DOCENTIA, sin sesión. */
export default function Presentacion() {
  return (
    <div className="presentacion">
      <header className="presentacion__cabecera">
        <Link to="/" className="presentacion__marca" aria-label="DOCENTIA, inicio">
          <span className="presentacion__marca-icono" aria-hidden="true">
            <GraduationCap size={22} />
          </span>
          <span>
            <strong>DOCENTIA</strong>
            <small>Evaluación docente</small>
          </span>
        </Link>
        <img
          className="presentacion__logo-institucional"
          src="/brand/logo-uniautonoma.png"
          alt="Universidad Autónoma del Cauca"
        />
        <Link to="/login" className="presentacion__boton presentacion__boton--claro">
          Iniciar sesión <ArrowRight size={17} aria-hidden="true" />
        </Link>
      </header>

      <main>
        <section className="presentacion__heroe">
          <div className="presentacion__heroe-texto">
            <p className="presentacion__antetitulo">Resolución 074 de 2026 · Universidad Autónoma del Cauca</p>
            <h1>
              Evaluación docente
              {' '}
              <span>de punta a punta</span>
            </h1>
            <p className="presentacion__descripcion">
              Un proceso institucional claro: integra las evaluaciones, organiza la retroalimentación y da seguimiento a
              los compromisos de cada periodo.
            </p>
            <div className="presentacion__acciones">
              <Link to="/login" className="presentacion__boton">
                Iniciar sesión <ArrowRight size={18} aria-hidden="true" />
              </Link>
              <a href="#como-funciona" className="presentacion__enlace">
                Conocer el proceso <ArrowDown size={16} aria-hidden="true" />
              </a>
            </div>
          </div>
          <div className="presentacion__visual" aria-label="Etapas del proceso DOCENTIA">
            <p className="presentacion__visual-rotulo">Un proceso conectado</p>
            <ol className="presentacion__ruta">
              {[
                ['01', 'Evaluar', 'Tres fuentes, un mismo periodo'],
                ['02', 'Analizar', 'Resultados y comentarios'],
                ['03', 'Retroalimentar', 'Reunión, acta y seguimiento'],
              ].map(([numero, titulo, texto]) => (
                <li key={numero}>
                  <span className="presentacion__ruta-numero">{numero}</span>
                  <span>
                    <strong>{titulo}</strong>
                    <small>{texto}</small>
                  </span>
                </li>
              ))}
            </ol>
            <div className="presentacion__visual-pie">
              <span aria-hidden="true"><UsersRound size={17} /></span>
              Evaluación con responsabilidades por rol
            </div>
          </div>
        </section>

        <section id="como-funciona" className="presentacion__seccion" aria-labelledby="titulo-pasos">
          <div className="presentacion__seccion-cabecera">
            <p className="presentacion__seccion-etiqueta">El recorrido</p>
            <h2 id="titulo-pasos">Cómo funciona</h2>
            <p>De la recolección de información al seguimiento de los compromisos.</p>
          </div>
          <ol className="presentacion__pasos">
            {PASOS.map(({ Icono, titulo, texto }, i) => (
              <li key={titulo} style={{ '--orden': i }}>
                <span className="presentacion__numero" aria-hidden="true">
                  <Icono size={21} />
                </span>
                <span className="presentacion__paso-etiqueta">Etapa 0{i + 1}</span>
                <h3>{titulo}</h3>
                <p>{texto}</p>
              </li>
            ))}
          </ol>
        </section>

        <section className="presentacion__seccion" aria-labelledby="titulo-publico">
          <div className="presentacion__seccion-cabecera presentacion__seccion-cabecera--compacta">
            <p className="presentacion__seccion-etiqueta">Un sistema por roles</p>
            <h2 id="titulo-publico">Cada persona, su parte del proceso</h2>
          </div>
          <ul className="presentacion__publico">
            {PUBLICO.map(([Icono, rol, texto], i) => (
              <li key={rol} style={{ '--orden': i }}>
                <span className="presentacion__publico-icono" aria-hidden="true">
                  <Icono size={20} />
                </span>
                <h3>{rol}</h3>
                <p>{texto}</p>
              </li>
            ))}
          </ul>
        </section>
      </main>

      <footer className="presentacion__pie">
        <span>DOCENTIA</span> · Universidad Autónoma del Cauca · Resolución 074 de 2026
      </footer>
    </div>
  )
}
