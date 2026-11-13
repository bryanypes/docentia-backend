import { Clock, PlayCircle } from 'lucide-react'
import robotPensando from '../../assets/ilustraciones/robot-pensando.svg'
import robotSenala from '../../assets/ilustraciones/robot-senala.svg'
import { TUTORIALES } from '../../shared/constants/tutoriales.js'
import { useTituloDocumento } from '../../shared/hooks/useTituloDocumento.js'
import { CabeceraPublica } from './CabeceraPublica.jsx'
import { EnlaceExterno, PiePublico } from './ComponentesPublicos.jsx'
import './Presentacion.css'

/** Videos tutoriales por rol; los que aún no se suben se muestran como pendientes. */
export default function Tutoriales() {
  useTituloDocumento('Tutoriales')
  const pendientes = TUTORIALES.some((t) => !t.url)

  return (
    <div className="presentacion">
      <CabeceraPublica enlaces={[['Inicio', '/']]} />
      <main className="presentacion__seccion tutoriales">
        <div className="tutoriales__cabecera">
          <img className="tutoriales__robot" src={robotSenala} alt="" />
          <div>
            <p className="presentacion__seccion-etiqueta">Tutoriales</p>
            <h1>Aprende a usar Evaluación Docente</h1>
            <p>Un video corto para cada rol, con los pasos de la plataforma.</p>
          </div>
        </div>

        {pendientes && (
          <p className="tutoriales__aviso" role="status">
            <Clock size={18} aria-hidden="true" />
            Estamos preparando los nuevos videos de Evaluación Docente. Pronto estarán disponibles aquí.
          </p>
        )}

        <ul className="tutoriales__lista">
          {TUTORIALES.map(({ rol, titulo, texto, url }) => (
            <li key={rol} className={`tutoriales__video${url ? '' : ' tutoriales__video--pendiente'}`}>
              <span className="tutoriales__miniatura" aria-hidden="true">
                <img src={robotPensando} alt="" />
                <PlayCircle size={34} className="tutoriales__play" />
              </span>
              <span className="tutoriales__rol">{rol}</span>
              <h2>{titulo}</h2>
              <p>{texto}</p>
              {url ? (
                <EnlaceExterno href={url} className="presentacion__boton presentacion__boton--claro">
                  Ver video
                </EnlaceExterno>
              ) : (
                <span className="tutoriales__pendiente">Pendiente · muy pronto</span>
              )}
            </li>
          ))}
        </ul>
      </main>
      <PiePublico />
    </div>
  )
}
