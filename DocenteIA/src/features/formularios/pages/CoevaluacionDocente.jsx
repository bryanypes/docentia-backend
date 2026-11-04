import { ChevronLeft, RefreshCw } from 'lucide-react'
import { useCallback } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
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
  const cargar = useCallback(() => obtenerCoevaluacion(idDocente), [idDocente])
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
        {datos && (
          <FormularioInstrumento
            key={`${datos.idDocente}-${datos.estado}`}
            formulario={datos}
            onGuardar={guardar}
            onEnviar={enviar}
          />
        )}
      </div>
    </>
  )
}
