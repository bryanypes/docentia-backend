import { ChevronLeft, RefreshCw } from 'lucide-react'
import { useCallback } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { listarPeriodos } from '../../periodos/services/periodosService.js'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { FormularioInstrumento } from '../components/FormularioInstrumento.jsx'
import {
  enviarCoevaluacion,
  guardarCoevaluacion,
  obtenerCoevaluacion,
} from '../services/formulariosService.js'

export default function CoevaluacionDocente() {
  const { idDocente } = useParams()
  const [parametros, setParametros] = useSearchParams()
  const periodo = parametros.get('periodo') ?? ''
  const periodos = useFetch(listarPeriodos)
  const cargar = useCallback(() => obtenerCoevaluacion(idDocente, periodo || null), [idDocente, periodo])
  const { datos, error, cargando, recargar } = useFetch(cargar, [cargar])
  const guardar = useCallback((borrador) => guardarCoevaluacion(idDocente, borrador), [idDocente])
  const enviar = useCallback(async () => {
    await enviarCoevaluacion(idDocente)
    recargar()
  }, [idDocente, recargar])
  const noEncontrado = error?.status === 404

  return (
    <>
      <Encabezado
        miga="Coordinación · Coevaluación"
        titulo={datos ? `Coevaluación de ${datos.nombreDocente}` : 'Coevaluación'}
        subtitulo="Seis ítems del Anexo 1. El docente nunca ve estos puntajes."
      />
      <div className="pagina">
        <Link to="/coevaluaciones" className="enlace-volver">
          <ChevronLeft size={16} aria-hidden="true" />
          Volver a coevaluaciones
        </Link>
        {periodos.datos?.length > 1 && (
          <label className="campo coevaluacion__periodo">
            <span className="rotulo rotulo--campo">Ver coevaluaciones anteriores</span>
            <select
              className="campo__control"
              value={periodo}
              onChange={(e) => setParametros(e.target.value ? { periodo: e.target.value } : {})}
            >
              <option value="">Periodo activo</option>
              {periodos.datos
                .filter((p) => !p.esActivo)
                .map((p) => (
                  <option key={p.idPeriodo} value={p.idPeriodo}>
                    {p.nombre}
                  </option>
                ))}
            </select>
          </label>
        )}
        {cargando && !datos && <Cargando texto="Cargando la coevaluación" bloques={2} />}
        {error && (
          <EstadoVista
            tipo={noEncontrado ? 'vacio' : 'error'}
            titulo={noEncontrado ? 'Formulario no encontrado' : 'No fue posible cargar el formulario'}
            mensaje={noEncontrado ? 'El docente no está dentro de tu alcance.' : error.message}
          >
            {!noEncontrado && <Boton icono={RefreshCw} onClick={recargar}>Intentar de nuevo</Boton>}
          </EstadoVista>
        )}
        {datos?.estado === 'ENVIADA' && (
          <Aviso tipo="info" titulo="Ya evaluaste a este docente">
            Abajo están las respuestas que enviaste; ya no se pueden cambiar.{' '}
            <Link to={`/docentes/${datos.idDocente}`}>Ver el expediente del docente</Link>
          </Aviso>
        )}
        {datos && (
          <FormularioInstrumento
            key={`${datos.idDocente}-${periodo}-${datos.estado}`}
            formulario={datos}
            onGuardar={guardar}
            onEnviar={enviar}
          />
        )}
      </div>
    </>
  )
}
