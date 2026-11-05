import robotAprobada from '../../../assets/ilustraciones/robot-aprobada.svg'
import robotError from '../../../assets/ilustraciones/robot-error.svg'
import robotEsperando from '../../../assets/ilustraciones/robot-esperando.svg'
import robotPensando from '../../../assets/ilustraciones/robot-pensando.svg'
import robotSaludo from '../../../assets/ilustraciones/robot-saludo.svg'
import robotSenala from '../../../assets/ilustraciones/robot-senala.svg'
import './DocenteIA.css'

const ROBOT = {
  aprobada: robotAprobada,
  error: robotError,
  esperando: robotEsperando,
  pensando: robotPensando,
  saludo: robotSaludo,
  senala: robotSenala,
}

/**
 * DocenteIA hablando: la ilustración del asistente con un globo de texto. `tono` colorea el globo
 * (azul por defecto; turquesa, violeta, coral o alerta) y `tamano` elige entre normal y compacto.
 */
export function MensajeIA({ robot = 'senala', titulo, tono = 'azul', tamano = 'normal', role, children }) {
  return (
    <div className={`mensaje-ia mensaje-ia--${tono} mensaje-ia--${tamano}`} role={role}>
      <img className="mensaje-ia__robot" src={ROBOT[robot]} alt="" />
      <div className="mensaje-ia__globo">
        {titulo && <strong className="mensaje-ia__titulo">{titulo}</strong>}
        {children}
      </div>
    </div>
  )
}

/** Solo la ilustración, pequeña, para acompañar el título de un panel. */
export function RobotIA({ robot = 'pensando', etiqueta }) {
  return <img className="robot-ia" src={ROBOT[robot]} alt={etiqueta ?? ''} title={etiqueta} />
}
