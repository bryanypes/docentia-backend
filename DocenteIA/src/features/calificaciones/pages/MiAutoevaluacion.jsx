import { RefreshCw, Save } from 'lucide-react'
import { useState } from 'react'
import { useVentanaEtapa } from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ASPECTOS, ETAPAS } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { aNumero, validarNotaAspecto } from '../../../shared/utils/validators.js'
import { AvisoEtapaCerrada } from '../../periodos/components/AvisoEtapaCerrada.jsx'
import { guardarAutoevaluacion, obtenerAutoevaluacion } from '../services/calificacionesService.js'

function Formulario({ guardada, cerrada, onGuardado }) {
  const [notas, setNotas] = useState(() =>
    Object.fromEntries(ASPECTOS.map(({ campo }) => [campo, guardada ? String(guardada[campo]).replace('.', ',') : ''])),
  )
  const [motivo, setMotivo] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const invalida = ASPECTOS.some(({ campo }) => validarNotaAspecto(notas[campo]))
  const cambio = guardada && ASPECTOS.some(({ campo }) => aNumero(notas[campo]) !== Number(guardada[campo]))

  async function enviar(evento) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      await guardarAutoevaluacion({
        ...Object.fromEntries(ASPECTOS.map(({ campo }) => [campo, aNumero(notas[campo])])),
        motivoCorreccion: cambio ? motivo.trim() : null,
      })
      onGuardado()
    } catch (errorApi) {
      setError(errorApi)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="formulario-simple" onSubmit={enviar}>
      {ASPECTOS.map(({ campo, nombre, guia }) => (
        <Campo key={campo} id={campo} etiqueta={nombre} ayuda={guia} error={notas[campo] && validarNotaAspecto(notas[campo])}>
          <input
            id={campo}
            className="campo__control"
            inputMode="decimal"
            required
            value={notas[campo]}
            onChange={(e) => setNotas((actuales) => ({ ...actuales, [campo]: e.target.value }))}
          />
        </Campo>
      ))}
      {cambio && (
        <Campo id="motivoAutoevaluacion" etiqueta="Motivo de la corrección">
          <textarea
            id="motivoAutoevaluacion"
            className="campo__control"
            maxLength={500}
            required
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
          />
        </Campo>
      )}
      <Boton type="submit" icono={Save} disabled={invalida || enviando || cerrada}>
        {enviando ? 'Guardando…' : 'Guardar autoevaluación'}
      </Boton>
      <ErrorApi error={error} />
    </form>
  )
}

export default function MiAutoevaluacion() {
  const autoevaluacion = useFetch(obtenerAutoevaluacion)
  const recoleccion = useVentanaEtapa(ETAPAS.RECOLECCION_CALIFICACIONES)
  const [guardado, setGuardado] = useState(0)

  return (
    <>
      <Encabezado
        miga="Docente · Mi autoevaluación"
        titulo="Mi autoevaluación"
        subtitulo="Pesa el 20 % de tu resultado por defecto. Califica cada aspecto de 1,0 a 5,0."
      />
      <div className="pagina">
        {guardado > 0 && <Aviso tipo="exito" titulo="Autoevaluación guardada" />}
        <AvisoEtapaCerrada ventana={recoleccion} accion="Autoevaluarte" />
        {autoevaluacion.cargando && !autoevaluacion.datos && <Cargando texto="Cargando tu autoevaluación" bloques={1} />}
        {autoevaluacion.error && (
          <EstadoVista tipo="error" titulo="No fue posible cargarla" mensaje={autoevaluacion.error.message}>
            <Boton icono={RefreshCw} onClick={autoevaluacion.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}
        {!autoevaluacion.cargando && !autoevaluacion.error && (
          <Panel titulo="Cinco aspectos">
            <Formulario
              key={guardado}
              guardada={autoevaluacion.datos}
              cerrada={Boolean(recoleccion && !recoleccion.abierta)}
              onGuardado={() => {
                setGuardado((v) => v + 1)
                autoevaluacion.recargar()
              }}
            />
          </Panel>
        )}
      </div>
    </>
  )
}
