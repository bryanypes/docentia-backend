import { Save, Send } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { formatearFechaHora } from '../../../shared/utils/formatters.js'
import { AsistentePasos, PreguntaEscala } from './AsistentePasos.jsx'
import './FormularioInstrumento.css'

export const INTERVALO_AUTOGUARDADO_MS = 30000

function respuestasDe(formulario) {
  const respuestas = {}
  for (const criterio of formulario.criterios) {
    for (const { codigo, valor } of criterio.descriptores) {
      if (valor !== null && valor !== undefined) respuestas[codigo] = valor
    }
  }
  return respuestas
}

function comentariosDe(formulario) {
  return Object.fromEntries(formulario.criterios.map(({ codigo, comentario }) => [codigo, comentario ?? '']))
}

/**
 * Instrumento del Anexo 1 por pasos: un criterio por paso y la revisión al final. Guarda el borrador
 * con el botón y cada 30 s si hubo cambios; enviar es irreversible y exige confirmación.
 */
export function FormularioInstrumento({ formulario, onGuardar, onEnviar }) {
  const [respuestas, setRespuestas] = useState(() => respuestasDe(formulario))
  const [comentarios, setComentarios] = useState(() => comentariosDe(formulario))
  const [confirmado, setConfirmado] = useState(false)
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState(null)
  const [guardadoEn, setGuardadoEn] = useState(null)
  const sinGuardar = useRef(false)
  // La referencia frena el doble clic y los choques con el autoguardado.
  const enCurso = useRef(false)
  const yaEnviado = useRef(false)
  const borrador = useRef({ respuestas, comentarios })
  useEffect(() => {
    borrador.current = { respuestas, comentarios }
  }, [respuestas, comentarios])

  const enviada = formulario.estado === 'ENVIADA'
  const editable = !enviada && formulario.ventanaAbierta
  const respondidos = Object.keys(respuestas).length
  const completo = respondidos === formulario.total

  const guardar = useCallback(async () => {
    await onGuardar(borrador.current)
    sinGuardar.current = false
    setGuardadoEn(new Date().toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' }))
  }, [onGuardar])

  const ejecutar = useCallback(async (accion) => {
    if (enCurso.current) return
    enCurso.current = true
    setOcupado(true)
    setError(null)
    try {
      await accion()
    } catch (errorApi) {
      setError(errorApi)
    } finally {
      enCurso.current = false
      setOcupado(false)
    }
  }, [])

  useEffect(() => {
    if (!editable) return undefined
    const temporizador = setInterval(() => {
      if (sinGuardar.current) ejecutar(guardar)
    }, INTERVALO_AUTOGUARDADO_MS)
    return () => clearInterval(temporizador)
  }, [editable, ejecutar, guardar])

  function responder(codigo, valor) {
    sinGuardar.current = true
    setRespuestas((actuales) => ({ ...actuales, [codigo]: valor }))
  }

  function comentar(codigo, texto) {
    sinGuardar.current = true
    setComentarios((actuales) => ({ ...actuales, [codigo]: texto }))
  }

  const pasos = formulario.criterios.map((criterio) => ({
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
            editable={editable}
            onResponder={responder}
          />
        ))}
        <label className="instrumento__comentario">
          Comentario sobre este criterio (opcional)
          <textarea
            className="campo__control"
            rows={2}
            maxLength={1000}
            disabled={!editable}
            value={comentarios[criterio.codigo]}
            onChange={(e) => comentar(criterio.codigo, e.target.value)}
          />
        </label>
      </fieldset>
    ),
  }))

  const revision = {
    titulo: enviada ? 'Resumen' : 'Revisar y enviar',
    contenido: (
      <>
        <ul className="revision">
          {pasos.map((paso) => (
            <li
              key={paso.codigo}
              className={`revision__criterio${paso.respondidas < paso.total ? ' revision__criterio--incompleto' : ''}`}
            >
              <span>
                <strong>{paso.titulo}</strong>
                <br />
                <span>
                  {paso.respondidas} de {paso.total} respondidos
                </span>
              </span>
              {paso.respondidas === paso.total ? (
                <span className="pildora pildora--turquesa">Completo</span>
              ) : (
                <span className="pildora pildora--alerta">Falta</span>
              )}
            </li>
          ))}
        </ul>
        <ErrorApi error={error} />
        {editable && (
          <div className="instrumento__acciones">
            <div className="acciones-fila">
              <Boton variante="contorno" icono={Save} disabled={ocupado} onClick={() => ejecutar(guardar)}>
                Guardar borrador
              </Boton>
              {guardadoEn && (
                <span className="campo__ayuda" role="status">
                  Borrador guardado a las {guardadoEn}
                </span>
              )}
            </div>
            <label className="instrumento__confirmacion">
              <input type="checkbox" checked={confirmado} onChange={(e) => setConfirmado(e.target.checked)} /> Revisé
              mis respuestas y entiendo que enviar es irreversible.
            </label>
            <Boton
              icono={Send}
              disabled={!completo || !confirmado || ocupado}
              onClick={() =>
                ejecutar(async () => {
                  if (yaEnviado.current) return
                  await guardar()
                  await onEnviar()
                  yaEnviado.current = true
                })
              }
            >
              Enviar formulario
            </Boton>
            {!completo && <span className="campo__ayuda">Responde todos los ítems para poder enviar.</span>}
          </div>
        )}
      </>
    ),
  }

  return (
    <div className="instrumento">
      {enviada && (
        <Aviso tipo="exito" titulo="Formulario enviado">
          Se envió el {formatearFechaHora(formulario.enviadaEn)}. Ya no se puede cambiar.
        </Aviso>
      )}
      {!enviada && !formulario.ventanaAbierta && (
        <Aviso tipo="alerta" titulo="La recolección está cerrada">
          Este formulario solo se responde en la ventana de recolección de calificaciones.
        </Aviso>
      )}
      <AsistentePasos pasos={pasos} revision={revision} etiqueta="Criterios del instrumento" />
    </div>
  )
}
