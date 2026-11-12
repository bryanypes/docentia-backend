import { useVentanaEtapa } from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { ETAPAS } from '../../../shared/constants/dominio.js'
import { mensajeVentanaCerrada } from '../estadoVentana.js'
import { Send, ShieldCheck } from 'lucide-react'
import { useRef, useState } from 'react'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { AsistentePasos, PreguntaEscala } from '../../formularios/components/AsistentePasos.jsx'
import '../../formularios/components/FormularioInstrumento.css'

/**
 * Formulario anónimo del estudiante: once ítems, un comentario opcional y un solo envío. No guarda
 * borradores para no dejar respuestas parciales asociadas a nadie.
 */
const MENSAJE_CERRADA = { titulo: 'La evaluación ya cerró', texto: 'La ventana de recolección de este periodo terminó.' }

export function FormularioEstudiante({ formulario, onEnviar }) {
  const [respuestas, setRespuestas] = useState({})
  const [comentario, setComentario] = useState('')
  const [confirmado, setConfirmado] = useState(false)
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState(null)
  const enCurso = useRef(false)

  const completo = Object.keys(respuestas).length === formulario.total
  const cerrada = mensajeVentanaCerrada(useVentanaEtapa(ETAPAS.RECOLECCION_CALIFICACIONES))
  const editable = formulario.estado === 'PENDIENTE' && formulario.ventanaAbierta

  async function enviar() {
    if (enCurso.current) return
    enCurso.current = true
    setOcupado(true)
    setError(null)
    try {
      await onEnviar({ respuestas, comentario: comentario.trim() || null })
    } catch (errorApi) {
      setError(errorApi)
      enCurso.current = false
    } finally {
      setOcupado(false)
    }
  }

  return (
    <div className="instrumento instrumento--estudiante">
      <Aviso tipo="info" titulo="Tus respuestas son anónimas">
        {formulario.aviso}
      </Aviso>
      {formulario.estado === 'ENVIADA' && (
        <Aviso tipo="exito" titulo="Ya enviaste esta evaluación">
          Gracias. Una evaluación solo se puede enviar una vez.
        </Aviso>
      )}
      {formulario.estado === 'PENDIENTE' && !formulario.ventanaAbierta && (
        <Aviso tipo="alerta" titulo={(cerrada ?? MENSAJE_CERRADA).titulo}>
          {(cerrada ?? MENSAJE_CERRADA).texto}
        </Aviso>
      )}

      {editable && (
        <AsistentePasos
          etiqueta="Criterios de la evaluación"
          pasos={formulario.criterios.map((criterio) => ({
            codigo: criterio.codigo,
            titulo: criterio.nombre,
            total: criterio.descriptores.length,
            respondidas: criterio.descriptores.filter((d) => respuestas[d.codigo] !== undefined).length,
            contenido: (
              <fieldset className="instrumento__criterio">
                <legend className="solo-lectores">{criterio.nombre}</legend>
                {criterio.descriptores.map((descriptor) => (
                  <PreguntaEscala
                    key={descriptor.codigo}
                    descriptor={descriptor}
                    escala={formulario.escala}
                    valor={respuestas[descriptor.codigo]}
                    editable
                    onResponder={(codigo, valor) => setRespuestas((actuales) => ({ ...actuales, [codigo]: valor }))}
                  />
                ))}
              </fieldset>
            ),
          }))}
          revision={{
            titulo: 'Revisar y enviar',
            contenido: (
              <>
                <label className="instrumento__comentario">
                  Comentario (opcional). No incluyas datos personales: ni tu nombre ni el de otras personas.
                  <textarea
                    className="campo__control"
                    rows={3}
                    maxLength={1000}
                    value={comentario}
                    onChange={(e) => setComentario(e.target.value)}
                  />
                </label>

                <ErrorApi error={error} />

                <div className="instrumento__acciones">
                  <label className="instrumento__confirmacion">
                    <input type="checkbox" checked={confirmado} onChange={(e) => setConfirmado(e.target.checked)} />{' '}
                    Revisé mis respuestas y entiendo que el envío es definitivo.
                  </label>
                  <Boton icono={Send} disabled={!completo || !confirmado || ocupado} onClick={enviar}>
                    {ocupado ? 'Enviando…' : 'Enviar evaluación'}
                  </Boton>
                  {!completo && (
                    <span className="campo__ayuda">Responde los {formulario.total} ítems para poder enviar.</span>
                  )}
                </div>
              </>
            ),
          }}
        />
      )}
    </div>
  )
}

export function Agradecimiento() {
  return (
    <Aviso tipo="exito" titulo="¡Gracias por tu evaluación!">
      <p>
        <ShieldCheck size={16} aria-hidden="true" /> Tu respuesta quedó registrada de forma anónima. Nadie puede ver
        qué respondiste.
      </p>
    </Aviso>
  )
}
