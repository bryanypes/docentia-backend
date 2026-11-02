import { Send, ShieldCheck } from 'lucide-react'
import { useRef, useState } from 'react'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import '../../formularios/components/FormularioInstrumento.css'

/**
 * Formulario anónimo del estudiante: once ítems, un comentario opcional y un solo envío. No guarda
 * borradores para no dejar respuestas parciales asociadas a nadie.
 */
export function FormularioEstudiante({ formulario, onEnviar }) {
  const [respuestas, setRespuestas] = useState({})
  const [comentario, setComentario] = useState('')
  const [confirmado, setConfirmado] = useState(false)
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState(null)
  const enCurso = useRef(false)

  const respondidos = Object.keys(respuestas).length
  const completo = respondidos === formulario.total
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
    <div className="instrumento">
      <Aviso tipo="info" titulo="Tus respuestas son anónimas">
        {formulario.aviso}
      </Aviso>
      {formulario.estado === 'ENVIADA' && (
        <Aviso tipo="exito" titulo="Ya enviaste esta evaluación">
          Gracias. Una evaluación solo se puede enviar una vez.
        </Aviso>
      )}
      {formulario.estado === 'PENDIENTE' && !formulario.ventanaAbierta && (
        <Aviso tipo="alerta" titulo="La evaluación está cerrada">
          Solo se responde durante la ventana de recolección de calificaciones.
        </Aviso>
      )}

      {editable && (
        <>
          <div className="instrumento__progreso">
            <span>
              {respondidos} de {formulario.total} ítems respondidos
            </span>
            <progress value={respondidos} max={formulario.total} aria-label="Avance de la evaluación" />
          </div>

          <dl className="instrumento__escala" aria-label="Escala de respuesta">
            {formulario.escala.map(({ valor, etiqueta }) => (
              <div key={valor}>
                <dt>{valor}</dt>
                <dd>{etiqueta}</dd>
              </div>
            ))}
          </dl>

          {formulario.criterios.map((criterio) => (
            <fieldset key={criterio.codigo} className="instrumento__criterio">
              <legend>{criterio.nombre}</legend>
              {criterio.descriptores.map((descriptor) => (
                <fieldset key={descriptor.codigo} className="instrumento__item">
                  <legend>{descriptor.texto}</legend>
                  <div className="instrumento__opciones">
                    {formulario.escala.map(({ valor, etiqueta }) => (
                      <label key={valor} className="instrumento__opcion">
                        <input
                          type="radio"
                          name={descriptor.codigo}
                          value={valor}
                          checked={respuestas[descriptor.codigo] === valor}
                          onChange={() => setRespuestas((actuales) => ({ ...actuales, [descriptor.codigo]: valor }))}
                        />
                        <span aria-hidden="true">{valor}</span>
                        <span className="solo-lectores">{etiqueta}</span>
                      </label>
                    ))}
                  </div>
                </fieldset>
              ))}
            </fieldset>
          ))}

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
              <input type="checkbox" checked={confirmado} onChange={(e) => setConfirmado(e.target.checked)} /> Revisé
              mis respuestas y entiendo que el envío es definitivo.
            </label>
            <Boton icono={Send} disabled={!completo || !confirmado || ocupado} onClick={enviar}>
              {ocupado ? 'Enviando…' : 'Enviar evaluación'}
            </Boton>
            {!completo && <span className="campo__ayuda">Responde los {formulario.total} ítems para poder enviar.</span>}
          </div>
        </>
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
