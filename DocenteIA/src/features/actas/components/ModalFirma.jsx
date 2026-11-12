import { Eraser, FileSignature, PenLine, Type } from 'lucide-react'
import { useEffect, useId, useRef, useState } from 'react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Modal } from '../../../shared/components/ui/Modal/Modal.jsx'
import './ModalFirma.css'

const ANCHO = 560
const ALTO = 190
const COLOR_TRAZO = '#0b1f4d'

/** Lienzo a la resolución de la pantalla (hasta 2x) para que el trazo no se vea pixelado. */
function prepararLienzo(lienzo) {
  const escala = Math.min(window.devicePixelRatio || 1, 2)
  lienzo.width = ANCHO * escala
  lienzo.height = ALTO * escala
  const contexto = lienzo.getContext('2d')
  if (!contexto) return null
  contexto.scale(escala, escala)
  contexto.lineCap = 'round'
  contexto.lineJoin = 'round'
  contexto.strokeStyle = COLOR_TRAZO
  contexto.fillStyle = COLOR_TRAZO
  return contexto
}

/**
 * Firma dibujada en la propia página. Se firma con mouse, dedo o lápiz; quien no puede dibujar
 * escribe su nombre y se dibuja con letra manuscrita. El PNG va al backend y al PDF del acta.
 */
export function ModalFirma({ nombre, tipoActa, enviando, error, onFirmar, onCerrar }) {
  const lienzo = useRef(null)
  const contexto = useRef(null)
  const ultimo = useRef(null)
  const [modo, setModo] = useState('dibujar')
  const [hayTrazo, setHayTrazo] = useState(false)
  const [texto, setTexto] = useState('')
  const [confirmado, setConfirmado] = useState(false)
  const idConfirmacion = useId()
  const idTexto = useId()
  const idAyuda = useId()

  useEffect(() => {
    contexto.current = prepararLienzo(lienzo.current)
  }, [])

  function borrar() {
    const ctx = contexto.current
    if (ctx) ctx.clearRect(0, 0, ANCHO, ALTO)
    setHayTrazo(false)
  }

  function punto(evento) {
    const caja = lienzo.current.getBoundingClientRect()
    return {
      x: ((evento.clientX - caja.left) / caja.width) * ANCHO,
      y: ((evento.clientY - caja.top) / caja.height) * ALTO,
      grosor: evento.pointerType === 'pen' && evento.pressure ? 1.2 + evento.pressure * 2.6 : 2.4,
    }
  }

  function empezar(evento) {
    if (modo !== 'dibujar' || !contexto.current) return
    evento.preventDefault()
    lienzo.current.setPointerCapture?.(evento.pointerId)
    const p = punto(evento)
    ultimo.current = p
    const ctx = contexto.current
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.grosor / 2, 0, Math.PI * 2)
    ctx.fill()
  }

  function trazar(evento) {
    if (!ultimo.current || !contexto.current) return
    const p = punto(evento)
    const ctx = contexto.current
    const medio = { x: (ultimo.current.x + p.x) / 2, y: (ultimo.current.y + p.y) / 2 }
    ctx.lineWidth = p.grosor
    ctx.beginPath()
    ctx.moveTo(ultimo.current.x, ultimo.current.y)
    ctx.quadraticCurveTo(ultimo.current.x, ultimo.current.y, medio.x, medio.y)
    ctx.lineTo(p.x, p.y)
    ctx.stroke()
    ultimo.current = p
    setHayTrazo(true)
  }

  function terminar() {
    ultimo.current = null
  }

  function escribir(valor) {
    setTexto(valor)
    const ctx = contexto.current
    if (!ctx) return
    ctx.clearRect(0, 0, ANCHO, ALTO)
    const limpio = valor.trim()
    if (!limpio) {
      setHayTrazo(false)
      return
    }
    let tamano = 58
    ctx.font = `italic 600 ${tamano}px "Segoe Script", "Brush Script MT", "Lucida Handwriting", cursive`
    while (ctx.measureText(limpio).width > ANCHO - 40 && tamano > 22) {
      tamano -= 2
      ctx.font = `italic 600 ${tamano}px "Segoe Script", "Brush Script MT", "Lucida Handwriting", cursive`
    }
    ctx.textBaseline = 'middle'
    ctx.fillText(limpio, 20, ALTO / 2)
    setHayTrazo(true)
  }

  function cambiarModo(nuevo) {
    setModo(nuevo)
    setTexto('')
    borrar()
  }

  function firmar() {
    if (!hayTrazo || !confirmado) return
    onFirmar(lienzo.current.toDataURL('image/png'))
  }

  return (
    <Modal
      titulo="Firmar el acta"
      descripcion={`${tipoActa ?? 'Acta'} · firma como ${nombre}. La firma queda en el PDF y no se puede deshacer.`}
      icono={FileSignature}
      onCerrar={onCerrar}
    >
      <div className="firma">
        <div className="firma__modos" role="group" aria-label="Cómo firmar">
          <button
            type="button"
            className={`firma__modo${modo === 'dibujar' ? ' firma__modo--activo' : ''}`}
            aria-pressed={modo === 'dibujar'}
            onClick={() => cambiarModo('dibujar')}
          >
            <PenLine size={16} aria-hidden="true" />
            Dibujar
          </button>
          <button
            type="button"
            className={`firma__modo${modo === 'escribir' ? ' firma__modo--activo' : ''}`}
            aria-pressed={modo === 'escribir'}
            onClick={() => cambiarModo('escribir')}
          >
            <Type size={16} aria-hidden="true" />
            Escribir
          </button>
        </div>

        {modo === 'escribir' && (
          <label className="firma__escribir" htmlFor={idTexto}>
            Tu nombre como firma
            <input
              id={idTexto}
              className="campo__control"
              value={texto}
              maxLength={60}
              autoComplete="name"
              onChange={(e) => escribir(e.target.value)}
            />
          </label>
        )}

        <div className={`firma__marco${modo === 'dibujar' ? ' firma__marco--dibujo' : ''}`}>
          <canvas
            ref={lienzo}
            className="firma__lienzo"
            role="img"
            aria-label={hayTrazo ? 'Firma lista' : 'Espacio para la firma, vacío'}
            aria-describedby={idAyuda}
            onPointerDown={empezar}
            onPointerMove={trazar}
            onPointerUp={terminar}
            onPointerCancel={terminar}
            onPointerLeave={terminar}
          />
          <span className="firma__linea" aria-hidden="true" />
          {!hayTrazo && (
            <span className="firma__guia" aria-hidden="true">
              {modo === 'dibujar' ? 'Firma aquí con el mouse, el dedo o un lápiz' : 'Escribe tu nombre arriba'}
            </span>
          )}
        </div>
        <p id={idAyuda} className="campo__ayuda">
          {modo === 'dibujar'
            ? 'Si no puedes dibujar, elige “Escribir”: tu nombre se convierte en el trazo de la firma.'
            : 'Se dibuja con letra manuscrita y queda igual en el PDF.'}
        </p>
        <p className="campo__ayuda firma__alcance">
          DOCENTIA registra el trazo, la cuenta y la fecha con un hash de integridad; no usa un certificado digital
          criptográfico.
        </p>

        <div className="firma__acciones-lienzo">
          <Boton variante="secundario" icono={Eraser} disabled={!hayTrazo} onClick={modo === 'escribir' ? () => escribir('') : borrar}>
            Borrar
          </Boton>
        </div>

        <label htmlFor={idConfirmacion} className="firma__confirmacion">
          <input
            id={idConfirmacion}
            type="checkbox"
            checked={confirmado}
            onChange={(e) => setConfirmado(e.target.checked)}
          />
          Confirmo que leí el acta y entiendo que esta firma queda registrada y no se puede retirar desde DOCENTIA.
        </label>

        <ErrorApi error={error} />

        <div className="firma__pie">
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton icono={FileSignature} disabled={!hayTrazo || !confirmado || enviando} onClick={firmar}>
            {enviando ? 'Firmando…' : 'Firmar acta'}
          </Boton>
        </div>
      </div>
    </Modal>
  )
}
