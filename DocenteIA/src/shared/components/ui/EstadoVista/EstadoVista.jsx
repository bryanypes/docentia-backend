import robotError from '../../../../assets/ilustraciones/robot-error.svg'
import robotEsperando from '../../../../assets/ilustraciones/robot-esperando.svg'
import './EstadoVista.css'

const ILUSTRACION = { error: robotError, vacio: robotEsperando }

/** Estado de error o vacío del maquetado, con ilustración, mensaje y acción opcional. */
export function EstadoVista({ tipo = 'vacio', titulo, mensaje, children }) {
  return (
    <div className={`estado-vista estado-vista--${tipo}`} role={tipo === 'error' ? 'alert' : undefined}>
      <span className="estado-vista__robot" aria-hidden="true">
        <img src={ILUSTRACION[tipo]} alt="" className="estado-vista__ilustracion" />
      </span>
      <strong className="estado-vista__titulo">{titulo}</strong>
      {mensaje && <p className="estado-vista__mensaje">{mensaje}</p>}
      {children}
    </div>
  )
}

/** Esqueleto de carga con anuncio para lectores de pantalla. */
export function Cargando({ texto = 'Cargando', bloques = 3 }) {
  return (
    <div className="cargando" role="status" aria-live="polite">
      <span className="solo-lectores">{texto}</span>
      {Array.from({ length: bloques }, (_, i) => (
        <div key={i} className="cargando__bloque" />
      ))}
    </div>
  )
}
