import { useVentanaEtapa } from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { ETAPAS } from '../../../shared/constants/dominio.js'
import { mensajeVentanaCerrada } from '../estadoVentana.js'
import { errorComentario } from '../validarComentario.js'
import { Send } from 'lucide-react'
import { useRef, useState } from 'react'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { AsistentePasos, PreguntaEscala } from '../../formularios/components/AsistentePasos.jsx'
import { EvaluacionRecibida } from '../../formularios/components/EvaluacionRecibida.jsx'
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
  const problemaComentario = errorComentario(comentario)
  const cerrada = mensajeVentanaCerrada(useVentanaEtapa(ETAPAS.RECOLECCION_CALIFICACIONES))
  const editable = formulario.estado === 'PENDIENTE' && formulario.ventanaAbierta

  async function enviar() {
    if (enCurso.current) return
    enCurso.current = true
    setOcupado(true)
    setError(null)
    try {
      await onEnviar({ respuestas, comentario: comentario.trim() })
    } catch (errorApi) {
      setError(errorApi)
      enCurso.current = false
    } finally {
      setOcupado(false)
    }
  }

  // Ya enviada (en otra visita): solo la confirmación con el resumen, sin los pasos.
  if (formulario.estado === 'ENVIADA') {
    return <Agradecimiento formulario={formulario} detalle="Ya enviaste esta evaluación. Una evaluación solo se puede enviar una vez." />
  }

  return (
    <div className="instrumento instrumento--estudiante">
      <Aviso tipo="info" titulo="Tus respuestas son anónimas">
        {formulario.aviso}
      </Aviso>
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
                  Comentario sobre el profesor (obligatorio, mínimo 10 caracteres). No incluyas datos personales: ni
                  tu nombre ni el de otras personas.
                  <textarea
                    className="campo__control"
                    rows={3}
                    maxLength={1000}
                    required
                    aria-invalid={comentario !== '' && Boolean(problemaComentario)}
                    aria-describedby="ayuda-comentario"
                    value={comentario}
                    onChange={(e) => setComentario(e.target.value)}
                  />
                </label>
                {comentario !== '' && problemaComentario && (
                  <span id="ayuda-comentario" className="campo__error" role="alert">
                    {problemaComentario}
                  </span>
                )}

                <ErrorApi error={error} />

                <div className="instrumento__acciones">
                  <label className="instrumento__confirmacion">
                    <input type="checkbox" checked={confirmado} onChange={(e) => setConfirmado(e.target.checked)} />{' '}
                    Revisé mis respuestas y entiendo que el envío es definitivo.
                  </label>
                  <Boton icono={Send} disabled={!completo || !confirmado || ocupado || Boolean(problemaComentario)} onClick={enviar}>
                    {ocupado ? 'Enviando…' : 'Enviar evaluación'}
                  </Boton>
                  {!completo && (
                    <span className="campo__ayuda">Responde los {formulario.total} ítems para poder enviar.</span>
                  )}
                  {completo && problemaComentario && (
                    <span className="campo__ayuda">Escribe tu comentario para poder enviar.</span>
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

/**
 * Confirmación del envío del estudiante: el avatar de IA, el resumen por criterio y la vuelta al
 * inicio. Al enviar se responden todos los ítems, así que cada criterio queda completo.
 */
export function Agradecimiento({
  formulario,
  detalle = '¡Gracias por tu evaluación! Quedó registrada de forma anónima: nadie puede ver qué respondiste.',
}) {
  const resumen = formulario.criterios.map((criterio) => ({
    codigo: criterio.codigo,
    titulo: criterio.nombre,
    respondidas: criterio.descriptores.length,
    total: criterio.descriptores.length,
  }))
  return <EvaluacionRecibida detalle={detalle} resumen={resumen} />
}
