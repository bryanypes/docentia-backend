import { ChevronLeft, RefreshCw } from 'lucide-react'
import { useCallback, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { Agradecimiento, FormularioEstudiante } from '../../estudiante/components/FormularioEstudiante.jsx'
import { enviarEvaluacion, obtenerEvaluacion } from '../../estudiante/services/estudianteService.js'

export default function HeteroevaluacionClase() {
  const { idAsignacion } = useParams()
  const cargar = useCallback(() => obtenerEvaluacion(idAsignacion), [idAsignacion])
  const { datos, error, cargando, recargar } = useFetch(cargar, [cargar])
  const [enviada, setEnviada] = useState(false)
  const enviar = useCallback(
    async (envio) => {
      await enviarEvaluacion(idAsignacion, envio)
      setEnviada(true)
    },
    [idAsignacion],
  )
  const noEncontrado = error?.status === 404

  return (
    <>
      <Encabezado
        miga="Estudiante · Evaluación"
        titulo={datos ? `Evaluación de ${datos.docente}` : 'Evaluación'}
        subtitulo={datos ? `${datos.curso} · Grupo ${datos.grupo}. Once ítems del Anexo 1.` : 'Once ítems del Anexo 1.'}
      />
      <div className="pagina">
        <Link to="/evaluacion-profesores" className="enlace-volver">
          <ChevronLeft size={16} aria-hidden="true" />
          Volver a mis evaluaciones
        </Link>
        {cargando && !datos && <Cargando texto="Cargando la evaluación" bloques={2} />}
        {error && (
          <EstadoVista
            tipo={noEncontrado ? 'vacio' : 'error'}
            titulo={noEncontrado ? 'Evaluación no encontrada' : 'No fue posible cargar la evaluación'}
            mensaje={noEncontrado ? 'Ese curso no está entre tus matrículas activas.' : error.message}
          >
            {!noEncontrado && <Boton icono={RefreshCw} onClick={recargar}>Intentar de nuevo</Boton>}
          </EstadoVista>
        )}
        {enviada && <Agradecimiento />}
        {datos && !enviada && <FormularioEstudiante formulario={datos} onEnviar={enviar} />}
      </div>
    </>
  )
}
