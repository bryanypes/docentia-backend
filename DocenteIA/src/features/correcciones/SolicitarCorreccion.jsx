import { LifeBuoy, Send } from 'lucide-react'
import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import { Boton } from '../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Modal } from '../../shared/components/ui/Modal/Modal.jsx'
import { useFetch } from '../../shared/hooks/useFetch.js'
import { formatearFechaHora } from '../../shared/utils/formatters.js'
import { listarMisSolicitudes, solicitarCorreccion } from './correccionesService.js'
import { ESTADO_SOLICITUD, MOTIVO_MINIMO, quienLaTiene } from './estados.js'
import './Correcciones.css'

/**
 * "¿Necesitas corregir esta evaluación?": abre un ticket que verifica el superior y atiende la
 * administración. Si ya hay uno abierto para este formulario, muestra en qué va.
 *
 * @param tipo 'AUTO' o 'COEVAL'
 * @param idDocente docente coevaluado (solo COEVAL)
 */
export function SolicitarCorreccion({ tipo, idDocente }) {
  const [abierto, setAbierto] = useState(false)
  const cargar = useCallback(() => listarMisSolicitudes(), [])
  const mias = useFetch(cargar, [cargar])
  const propia = (mias.datos ?? []).find(
    (s) => s.tipo === tipo && (tipo === 'AUTO' || s.idDocente === Number(idDocente)),
  )
  const enCurso = propia && (propia.estado === 'PENDIENTE_APROBACION' || propia.estado === 'APROBADA')

  return (
    <div className="correccion-pedir">
      {enCurso ? (
        <p className="correccion-pedir__estado" role="status">
          <span className={`pildora pildora--${ESTADO_SOLICITUD[propia.estado].clase}`}>
            {ESTADO_SOLICITUD[propia.estado].etiqueta}
          </span>{' '}
          Solicitud de corrección enviada el {formatearFechaHora(propia.fechaSolicitud)}. {quienLaTiene(propia)}{' '}
          <Link to="/solicitudes-correccion">Ver mis solicitudes</Link>
        </p>
      ) : (
        <Boton variante="contorno" icono={LifeBuoy} onClick={() => setAbierto(true)}>
          Necesito corregir esta evaluación
        </Boton>
      )}
      {abierto && (
        <ModalSolicitud
          tipo={tipo}
          idDocente={idDocente}
          onCerrar={() => setAbierto(false)}
          onEnviada={() => {
            setAbierto(false)
            mias.recargar()
          }}
        />
      )}
    </div>
  )
}

function ModalSolicitud({ tipo, idDocente, onCerrar, onEnviada }) {
  const [motivo, setMotivo] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState(null)
  const listo = motivo.trim().length >= MOTIVO_MINIMO
  const id = `motivo-correccion-${tipo}-${idDocente ?? 'propia'}`

  async function enviar(evento) {
    evento.preventDefault()
    if (!listo || enviando) return
    setEnviando(true)
    setError(null)
    try {
      await solicitarCorreccion({ tipo, idDocente: tipo === 'COEVAL' ? Number(idDocente) : null, motivo: motivo.trim() })
      onEnviada()
    } catch (errorApi) {
      setError(errorApi)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Modal
      titulo="Solicitar corrección"
      descripcion="Tu superior verifica la solicitud y, si la aprueba, la administración reabre el formulario para que lo corrijas."
      icono={LifeBuoy}
      ancho="pequeno"
      onCerrar={onCerrar}
    >
      <form className="correccion-form" onSubmit={enviar}>
        <ol className="correccion-form__pasos">
          <li>Explicas qué necesitas corregir.</li>
          <li>{tipo === 'AUTO' ? 'Tu coordinación (o la vicerrectoría)' : 'Tu superior'} verifica la solicitud.</li>
          <li>La administración reabre el formulario y te avisamos aquí y por correo.</li>
        </ol>
        <Campo id={id} etiqueta="¿Qué necesitas corregir y por qué?" ayuda={`Al menos ${MOTIVO_MINIMO} caracteres.`}>
          <textarea
            id={id}
            className="campo__control"
            rows={4}
            maxLength={500}
            required
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
          />
        </Campo>
        <ErrorApi error={error} />
        <div className="acciones-fila">
          <Boton variante="contorno" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" icono={Send} disabled={!listo || enviando}>
            {enviando ? 'Enviando…' : 'Enviar solicitud'}
          </Boton>
        </div>
      </form>
    </Modal>
  )
}
