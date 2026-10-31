import { RefreshCw, Scale } from 'lucide-react'
import { useState } from 'react'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { guardarPonderacionActiva, obtenerPonderacionActiva } from '../services/ponderacionesService.js'

const FUENTES = [
  { campo: 'pesoEstudiantes', etiqueta: 'Estudiantes' },
  { campo: 'pesoCoordinacion', etiqueta: 'Coordinación' },
  { campo: 'pesoVicerrectoria', etiqueta: 'Vicerrectoría' },
]
const NOMBRES_CAMPOS = Object.fromEntries(FUENTES.map(({ campo, etiqueta }) => [campo, etiqueta]))

export function FormularioPonderacion({ guardada, onGuardar }) {
  const [pesos, setPesos] = useState(() =>
    Object.fromEntries(FUENTES.map(({ campo }) => [campo, String(guardada[campo])])),
  )
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const suma = FUENTES.reduce((total, { campo }) => total + Number(pesos[campo] || 0), 0)
  const sumaCorrecta = Math.abs(suma - 100) < 0.001

  async function enviar(evento) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      await onGuardar(Object.fromEntries(FUENTES.map(({ campo }) => [campo, Number(pesos[campo])])))
    } catch (errorApi) {
      setError(errorApi)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="formulario-simple" onSubmit={enviar}>
      {FUENTES.map(({ campo, etiqueta }) => (
        <Campo key={campo} id={campo} etiqueta={`${etiqueta} (%)`}>
          <input
            id={campo}
            type="number"
            min="0"
            max="100"
            step="0.01"
            className="campo__control"
            value={pesos[campo]}
            onChange={(e) => setPesos((p) => ({ ...p, [campo]: e.target.value }))}
            required
          />
        </Campo>
      ))}
      <p className={`pildora pildora--${sumaCorrecta ? 'exito' : 'error'}`} role="status">
        Suma: {suma.toLocaleString('es-CO')} %
      </p>
      <Boton type="submit" disabled={!sumaCorrecta || enviando}>
        {enviando ? 'Guardando…' : 'Guardar ponderación'}
      </Boton>
      <ErrorApi error={error} nombresCampos={NOMBRES_CAMPOS} />
    </form>
  )
}

export default function Ponderaciones() {
  const ponderacion = useFetch(obtenerPonderacionActiva)
  const [resultado, setResultado] = useState(null)
  const [version, setVersion] = useState(0)

  async function guardar(pesos) {
    setResultado(null)
    await guardarPonderacionActiva(pesos)
    setResultado('Ponderación guardada. Los promedios del periodo se recalcularon.')
    setVersion((v) => v + 1)
    ponderacion.recargar()
  }

  const { datos, error, cargando } = ponderacion

  return (
    <>
      <Encabezado
        miga="Vicerrectoría · Ponderaciones"
        titulo="Ponderaciones"
        subtitulo="Peso de cada fuente en el promedio del periodo activo. Debe sumar 100 %."
      />
      <div className="pagina">
        {resultado && <Aviso tipo="exito" titulo={resultado} />}
        {cargando && !datos && <Cargando texto="Cargando ponderación" bloques={1} />}
        {error?.status === 409 && (
          <EstadoVista titulo="No hay un periodo activo" mensaje="Activa un periodo para configurar sus pesos." />
        )}
        {error && error.status !== 409 && (
          <EstadoVista tipo="error" titulo="No fue posible cargar la ponderación" mensaje={error.message}>
            <Boton icono={RefreshCw} onClick={ponderacion.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}
        {datos && (
          <Panel
            titulo={`Periodo ${datos.periodo.nombre}`}
            descripcion={datos.esPredeterminada ? 'Usa los pesos predeterminados.' : 'Usa pesos configurados.'}
            icono={Scale}
          >
            <FormularioPonderacion key={version} guardada={datos} onGuardar={guardar} />
          </Panel>
        )}
      </div>
    </>
  )
}
