import {
  ArrowRight,
  BookOpen,
  EyeOff,
  LifeBuoy,
  MessageSquareText,
  Phone,
  PlayCircle,
  ShieldCheck,
  Sparkles,
  UserRoundX,
  UsersRound,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import robotAprobada from '../../assets/ilustraciones/robot-aprobada.svg'
import robotEsperando from '../../assets/ilustraciones/robot-esperando.svg'
import robotPensando from '../../assets/ilustraciones/robot-pensando.svg'
import robotSaludo from '../../assets/ilustraciones/robot-saludo.svg'
import robotSenala from '../../assets/ilustraciones/robot-senala.svg'
import { SERVICIOS_NOVA, TICKETS_NOVA } from '../../shared/constants/ayuda.js'
import { CabeceraPublica } from './CabeceraPublica.jsx'
import { EnlaceExterno, PiePublico } from './ComponentesPublicos.jsx'
import './Presentacion.css'

const GARANTIAS = [
  [EyeOff, 'Nadie sabe quién escribió', 'Ni tu profesor, ni la coordinación, ni la Vicerrectoría ven quién escribió cada comentario.'],
  [UserRoundX, 'Sin datos personales', 'Antes de analizar los comentarios se quitan nombres, correos y todo lo que pueda identificarte.'],
  [UsersRound, 'Protege a los grupos pequeños', 'Los resultados solo se muestran cuando hay suficientes respuestas.'],
]

const PASOS = [
  [robotPensando, 'Evalúas', 'Estudiantes, coordinación y cada docente responden un cuestionario corto.'],
  [robotSenala, 'DocenteIA analiza', 'Reúne los resultados y agrupa los comentarios en fortalezas y oportunidades.'],
  [robotAprobada, 'Se acompaña', 'Cada docente conversa sus resultados y acuerda cómo seguir creciendo.'],
]

const TICKETS = [
  ['Estudiantes', TICKETS_NOVA.ESTUDIANTE],
  ['Docentes', TICKETS_NOVA.DOCENTE],
  ['Coordinación y demás', TICKETS_NOVA.GENERAL],
  ['Administrativos de área', TICKETS_NOVA.ADMINISTRATIVO],
]

const SERVICIOS = [
  [MessageSquareText, 'Radicar una PQRS', SERVICIOS_NOVA.PQRS],
  [Phone, 'Directorio de contactos', SERVICIOS_NOVA.CONTACTOS],
  [BookOpen, 'Manual de NOVA', SERVICIOS_NOVA.MANUAL],
]

/** Presentación pública de DOCENTIA, sin sesión. */
export default function Presentacion() {
  return (
    <div className="presentacion">
      <CabeceraPublica
        enlaces={[
          ['Anonimato', '#anonimato'],
          ['Cómo funciona', '#como-funciona'],
          ['Tutoriales', '/tutoriales'],
          ['Ayuda', '#ayuda'],
        ]}
      />

      <main>
        <section className="presentacion__heroe">
          <div className="presentacion__heroe-texto">
            <img
              className="presentacion__logo-heroe"
              src={`${import.meta.env.BASE_URL}brand/logo-uniautonoma.png`}
              alt=""
            />
            <p className="presentacion__antetitulo">Evaluación Docente · Autónoma del Cauca</p>
            <h1>
              Tu voz mejora <span>la enseñanza</span>
            </h1>
            <p className="presentacion__descripcion">
              DOCENTIA reúne la mirada de estudiantes, coordinación y docentes para acompañar el crecimiento de cada
              profesor. Tus comentarios <strong>siempre son anónimos</strong>.
            </p>
            <div className="presentacion__acciones">
              <Link to="/login" className="presentacion__boton">
                Iniciar sesión <ArrowRight size={18} aria-hidden="true" />
              </Link>
              <Link to="/tutoriales" className="presentacion__boton presentacion__boton--contorno">
                <PlayCircle size={18} aria-hidden="true" /> Ver tutoriales
              </Link>
            </div>
          </div>
          <div className="presentacion__retrato" aria-hidden="true">
            <img src={robotSaludo} alt="" />
          </div>
        </section>

        <ul className="presentacion__pilares" aria-label="Lo esencial">
          <li>
            <ShieldCheck size={22} aria-hidden="true" />
            <span><strong>100 % anónimo</strong>Nadie ve quién escribió cada comentario.</span>
          </li>
          <li>
            <Sparkles size={22} aria-hidden="true" />
            <span><strong>Con DocenteIA</strong>Los comentarios se resumen en hallazgos claros.</span>
          </li>
          <li>
            <UsersRound size={22} aria-hidden="true" />
            <span><strong>Para toda la comunidad</strong>Estudiantes, docentes y directivos.</span>
          </li>
        </ul>

        <section id="anonimato" className="presentacion__anonimato" aria-labelledby="titulo-anonimato">
          <div className="presentacion__anonimato-texto">
            <img className="presentacion__anonimato-robot" src={robotAprobada} alt="" />
            <p className="presentacion__seccion-etiqueta presentacion__seccion-etiqueta--clara">Lo más importante</p>
            <h2 id="titulo-anonimato">Tus comentarios siempre son anónimos</h2>
            <p>Escribe con confianza y con respeto: lo que dices ayuda a mejorar las clases.</p>
          </div>
          <ul className="presentacion__garantias">
            {GARANTIAS.map(([Icono, titulo, texto]) => (
              <li key={titulo}>
                <span className="presentacion__garantia-icono" aria-hidden="true">
                  <Icono size={20} />
                </span>
                <span>
                  <h3>{titulo}</h3>
                  <p>{texto}</p>
                </span>
              </li>
            ))}
          </ul>
        </section>

        <section id="como-funciona" className="presentacion__seccion" aria-labelledby="titulo-pasos">
          <div className="presentacion__seccion-cabecera">
            <p className="presentacion__seccion-etiqueta">El recorrido</p>
            <h2 id="titulo-pasos">Cómo funciona</h2>
          </div>
          <ol className="presentacion__pasos">
            {PASOS.map(([robot, titulo, texto], i) => (
              <li key={titulo}>
                <span className="presentacion__paso-robot" aria-hidden="true">
                  <img src={robot} alt="" />
                </span>
                <span>
                  <span className="presentacion__paso-numero">Paso {i + 1}</span>
                  <h3>{titulo}</h3>
                  <p>{texto}</p>
                </span>
              </li>
            ))}
          </ol>
        </section>

        <section className="presentacion__tutoriales" aria-labelledby="titulo-tutoriales">
          <img className="presentacion__tutoriales-robot" src={robotSenala} alt="" />
          <div>
            <h2 id="titulo-tutoriales">¿No sabes cómo usar Evaluación Docente?</h2>
            <p>Te llevamos a los videos tutoriales, uno por cada rol. Estamos subiendo los nuevos videos.</p>
          </div>
          <Link to="/tutoriales" className="presentacion__boton presentacion__boton--oro">
            <PlayCircle size={18} aria-hidden="true" /> Ver tutoriales
          </Link>
        </section>

        <section id="ayuda" className="presentacion__seccion" aria-labelledby="titulo-ayuda">
          <div className="presentacion__buzon">
            <div className="presentacion__buzon-intro">
              <img className="presentacion__buzon-robot" src={robotEsperando} alt="" />
              <div>
                <p className="presentacion__seccion-etiqueta">Buzón de ayuda</p>
                <h2 id="titulo-ayuda">¿Tienes dudas, encontraste un error o necesitas ayuda?</h2>
                <p>
                  Escríbenos por <strong>NOVA</strong>, el portal de trámites de la Corporación. Abre un ticket según
                  tu perfil y el equipo de soporte te responderá.
                </p>
              </div>
            </div>
            <div className="presentacion__buzon-tickets">
              <p className="presentacion__buzon-rotulo">
                <LifeBuoy size={18} aria-hidden="true" /> Abrir un ticket de soporte
              </p>
              <ul>
                {TICKETS.map(([perfil, url]) => (
                  <li key={perfil}>
                    <EnlaceExterno href={url} className="presentacion__ticket-enlace">
                      {perfil}
                    </EnlaceExterno>
                  </li>
                ))}
              </ul>
              <ul className="presentacion__servicios">
                {SERVICIOS.map(([Icono, titulo, url]) => (
                  <li key={titulo}>
                    <EnlaceExterno href={url} className="presentacion__servicio">
                      <Icono size={16} aria-hidden="true" /> {titulo}
                    </EnlaceExterno>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </section>
      </main>

      <PiePublico />
    </div>
  )
}
