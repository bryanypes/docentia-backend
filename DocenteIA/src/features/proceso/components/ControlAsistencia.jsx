import { UserCheck, UserX } from 'lucide-react'
import { useRef, useState } from 'react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { registrarAsistencia } from '../services/procesoService.js'
import './Proceso.css'

/**
 * Botones de la coordinación para registrar si el docente asistió. Asistió habilita al docente su
 * resultado y el acta; No asistió libera la cita.
 */
export function ControlAsistencia({ cita, nombreDocente, onRegistrada }) {
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const enCurso = useRef(false)

  async function registrar(asistio) {
    if (enCurso.current) return
    if (
      !asistio &&
      !window.confirm(`¿Registrar que ${nombreDocente ?? 'el docente'} no asistió? La cita se liberará y deberá reservar otra.`)
    ) {
      return
    }
    enCurso.current = true
    setEnviando(true)
    setError(null)
    try {
      const actualizada = await registrarAsistencia(cita.idCita, asistio)
      onRegistrada?.(actualizada)
    } catch (errorApi) {
      setError(errorApi)
    } finally {
      enCurso.current = false
      setEnviando(false)
    }
  }

  return (
    <div className="proceso__asistencia">
      <div className="acciones-fila">
        <Boton icono={UserCheck} disabled={enviando} onClick={() => registrar(true)}>
          Asistió · habilitar resultado
        </Boton>
        <Boton variante="contorno" icono={UserX} disabled={enviando} onClick={() => registrar(false)}>
          No asistió
        </Boton>
      </div>
      <ErrorApi error={error} />
    </div>
  )
}
