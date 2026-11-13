import {
  ArrowDown,
  ArrowRight,
  BookOpen,
  ChartColumn,
  CircleCheck,
  ClipboardCheck,
  ExternalLink,
  EyeOff,
  GraduationCap,
  LifeBuoy,
  MessageSquareText,
  Phone,
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
import { SERVICIOS_NOVA, TICKETS_NOVA, URL_NOVA } from '../../shared/constants/ayuda.js'
import './Presentacion.css'

const GARANTIAS = [
  [EyeOff, 'Nadie sabe quién escribió', 'Ni tu profesor, ni la coordinación, ni la Vicerrectoría pueden ver quién escribió cada comentario.'],
  [UserRoundX, 'Sin datos personales', 'Antes de analizar los comentarios se quitan nombres, correos y cualquier dato que pueda identificarte.'],
  [UsersRound, 'Protege a los grupos pequeños', 'Los resultados solo se muestran cuando hay suficientes respuestas para que nadie se reconozca.'],
]

const PASOS = [
  {
    robot: robotPensando,
    titulo: 'Evalúas',
    texto: 'Estudiantes, coordinación y cada docente responden un cuestionario corto, paso a paso, desde cualquier dispositivo.',
  },
  {
    robot: robotSenala,
    titulo: 'DocenteIA analiza',
    texto: 'Los resultados se reúnen y DocenteIA agrupa los comentarios en fortalezas y oportunidades de mejora.',
  },
  {
    robot: robotAprobada,
    titulo: 'Se acompaña al docente',
    texto: 'Cada docente conversa sus resultados con la coordinación y acuerdan juntos los pasos para seguir creciendo.',
  },
]

const ROLES = [
  [UsersRound, 'Estudiantes', 'Evalúan a sus profesores en pocos minutos y de forma anónima.'],
  [GraduationCap, 'Docentes', 'Se autoevalúan, reciben su retroalimentación y siguen sus compromisos.'],
  [ClipboardCheck, 'Coordinación', 'Acompaña a sus docentes, revisa los comentarios y conduce cada reunión.'],
  [ChartColumn, 'Vicerrectoría', 'Sigue la evaluación de toda la universidad con indicadores claros.'],
]

const TICKETS = [
  ['Estudiantes', TICKETS_NOVA.ESTUDIANTE],
  ['Docentes', TICKETS_NOVA.DOCENTE],
  ['Coordinación y demás', TICKETS_NOVA.GENERAL],
  ['Administrativos de área', TICKETS_NOVA.ADMINISTRATIVO],
]

const SERVICIOS = [
  [MessageSquareText, 'Radicar una PQRS', 'Peticiones, quejas, reclamos, sugerencias o felicitaciones, con número de radicado.', SERVICIOS_NOVA.PQRS],
  [Phone, 'Directorio de contactos', 'Encuentra a la dependencia que puede ayudarte.', SERVICIOS_NOVA.CONTACTOS],
  [BookOpen, 'Manual de NOVA', 'Aprende a abrir y seguir tus solicitudes en el portal.', SERVICIOS_NOVA.MANUAL],
]

function EnlaceExterno({ href, className, children }) {
  return (
    <a href={href} className={className} target="_blank" rel="noopener noreferrer">
      {children}
      <ExternalLink size={14} strokeWidth={2.25} aria-hidden="true" />
      <span className="solo-lectores">(se abre en otra pestaña)</span>
    </a>
  )
}

/** Presentación pública de DOCENTIA, sin sesión. */
export default function Presentacion() {
  return (
    <div className="presentacion">
      <header className="presentacion__cabecera">
        <Link to="/" className="presentacion__marca" aria-label="DOCENTIA, inicio">
          <span className="presentacion__marca-icono" aria-hidden="true">
            <img src={robotSaludo} alt="" />
          </span>
          <span>
            <strong>DOCENTIA</strong>
            <small>Evaluación docente</small>
          </span>
        </Link>
        <nav className="presentacion__nav" aria-label="Secciones">
          <a href="#anonimato">Anonimato</a>
          <a href="#como-funciona">Cómo funciona</a>
          <a href="#ayuda">Ayuda</a>
        </nav>
        <Link to="/login" className="presentacion__boton presentacion__boton--claro">
          Iniciar sesión <ArrowRight size={17} aria-hidden="true" />
        </Link>
      </header>

      <main>
        <section className="presentacion__heroe">
          <div className="presentacion__heroe-texto">
            <img
              className="presentacion__logo-institucional"
              src={`${import.meta.env.BASE_URL}brand/logo-uniautonoma.png`}
              alt="Universidad Autónoma del Cauca"
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
              <a href="#como-funciona" className="presentacion__enlace">
                ¿Cómo funciona? <ArrowDown size={16} aria-hidden="true" />
              </a>
            </div>
            <ul className="presentacion__sellos" aria-label="Lo esencial">
              <li><ShieldCheck size={16} aria-hidden="true" /> 100 % anónimo</li>
              <li><Sparkles size={16} aria-hidden="true" /> Con DocenteIA</li>
              <li><CircleCheck size={16} aria-hidden="true" /> Resolución 074</li>
            </ul>
          </div>

          <div className="presentacion__escena" aria-hidden="true">
            <div className="presentacion__halo" />
            <div className="presentacion__robot-principal">
              <img src={robotSaludo} alt="" />
            </div>
            <div className="presentacion__flotante presentacion__flotante--1">
              <span className="presentacion__flotante-icono"><ShieldCheck size={18} /></span>
              <span><strong>Comentario anónimo</strong><small>Nadie ve quién lo escribió</small></span>
            </div>
            <div className="presentacion__flotante presentacion__flotante--2">
              <span className="presentacion__flotante-icono presentacion__flotante-icono--oro"><Sparkles size={18} /></span>
              <span><strong>DocenteIA resume</strong><small>Fortalezas y oportunidades</small></span>
            </div>
            <div className="presentacion__flotante presentacion__flotante--3">
              <span className="presentacion__flotante-icono presentacion__flotante-icono--verde"><CircleCheck size={18} /></span>
              <span><strong>Acuerdos claros</strong><small>Con cada docente</small></span>
            </div>
          </div>
        </section>

        <section id="anonimato" className="presentacion__anonimato" aria-labelledby="titulo-anonimato">
          <div className="presentacion__anonimato-cabecera">
            <img className="presentacion__anonimato-robot" src={robotAprobada} alt="" />
            <div>
              <p className="presentacion__seccion-etiqueta presentacion__seccion-etiqueta--clara">Lo más importante</p>
              <h2 id="titulo-anonimato">Tus comentarios siempre son anónimos</h2>
              <p>Escribe con confianza y con respeto: lo que dices ayuda a mejorar las clases, nunca se vuelve en tu contra.</p>
            </div>
          </div>
          <ul className="presentacion__garantias">
            {GARANTIAS.map(([Icono, titulo, texto], i) => (
              <li key={titulo} style={{ '--orden': i }}>
                <span className="presentacion__garantia-icono" aria-hidden="true">
                  <Icono size={22} />
                </span>
                <h3>{titulo}</h3>
                <p>{texto}</p>
              </li>
            ))}
          </ul>
        </section>

        <section id="como-funciona" className="presentacion__seccion" aria-labelledby="titulo-pasos">
          <div className="presentacion__seccion-cabecera">
            <p className="presentacion__seccion-etiqueta">El recorrido</p>
            <h2 id="titulo-pasos">Cómo funciona</h2>
            <p>Tres momentos sencillos en cada periodo académico.</p>
          </div>
          <ol className="presentacion__pasos">
            {PASOS.map(({ robot, titulo, texto }, i) => (
              <li key={titulo} style={{ '--orden': i }}>
                <span className="presentacion__paso-numero" aria-hidden="true">{i + 1}</span>
                <span className="presentacion__paso-robot" aria-hidden="true">
                  <img src={robot} alt="" />
                </span>
                <h3>{titulo}</h3>
                <p>{texto}</p>
              </li>
            ))}
          </ol>
        </section>

        <section className="presentacion__seccion presentacion__seccion--suave" aria-labelledby="titulo-roles">
          <div className="presentacion__seccion-cabecera">
            <p className="presentacion__seccion-etiqueta">Para toda la comunidad</p>
            <h2 id="titulo-roles">Cada persona, su parte del proceso</h2>
          </div>
          <ul className="presentacion__roles">
            {ROLES.map(([Icono, rol, texto], i) => (
              <li key={rol} style={{ '--orden': i }}>
                <span className="presentacion__rol-icono" aria-hidden="true">
                  <Icono size={22} />
                </span>
                <h3>{rol}</h3>
                <p>{texto}</p>
              </li>
            ))}
          </ul>
        </section>

        <section id="ayuda" className="presentacion__seccion" aria-labelledby="titulo-ayuda">
          <div className="presentacion__buzon">
            <div className="presentacion__buzon-intro">
              <img className="presentacion__buzon-robot" src={robotEsperando} alt="" />
              <p className="presentacion__seccion-etiqueta">Buzón de ayuda</p>
              <h2 id="titulo-ayuda">¿Tienes dudas, encontraste un error o necesitas ayuda?</h2>
              <p>
                Escríbenos a través de <strong>NOVA</strong>, el portal de trámites y servicios de la Corporación. Abre un
                ticket según tu perfil y el equipo de soporte te responderá.
              </p>
            </div>

            <div className="presentacion__buzon-tarjetas">
              <article className="presentacion__ticket">
                <span className="presentacion__ticket-icono" aria-hidden="true">
                  <LifeBuoy size={22} />
                </span>
                <h3>Abrir un ticket de soporte</h3>
                <p>Elige tu perfil para ir directo a tus tickets en NOVA.</p>
                <ul className="presentacion__ticket-perfiles">
                  {TICKETS.map(([perfil, url]) => (
                    <li key={perfil}>
                      <EnlaceExterno href={url} className="presentacion__ticket-enlace">
                        {perfil}
                      </EnlaceExterno>
                    </li>
                  ))}
                </ul>
              </article>
              <ul className="presentacion__servicios">
                {SERVICIOS.map(([Icono, titulo, texto, url]) => (
                  <li key={titulo}>
                    <EnlaceExterno href={url} className="presentacion__servicio">
                      <span className="presentacion__servicio-icono" aria-hidden="true">
                        <Icono size={20} />
                      </span>
                      <span>
                        <strong>{titulo}</strong>
                        <small>{texto}</small>
                      </span>
                    </EnlaceExterno>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </section>
      </main>

      <footer className="presentacion__pie">
        <span>
          <strong>DOCENTIA</strong> · Evaluación Docente · Universidad Autónoma del Cauca
        </span>
        <span className="presentacion__pie-enlaces">
          <EnlaceExterno href={SERVICIOS_NOVA.POLITICA_DATOS} className="presentacion__pie-enlace">
            Política de tratamiento de datos
          </EnlaceExterno>
          <EnlaceExterno href={URL_NOVA} className="presentacion__pie-enlace">
            Portal NOVA
          </EnlaceExterno>
        </span>
      </footer>
    </div>
  )
}
