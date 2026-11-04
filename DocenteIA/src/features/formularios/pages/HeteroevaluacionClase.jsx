import { ChevronLeft, RefreshCw } from 'lucide-react'
import { useCallback, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { useSesion } from '../../auth/sesionContext.js'
import { Agradecimiento, FormularioEstudiante } from '../../estudiante/components/FormularioEstudiante.jsx'
import { ResumenEstudiante } from '../../estudiante/components/ResumenEstudiante.jsx'
import { guardarResumen, leerResumen } from '../../estudiante/resumenLocal.js'
import { enviarEvaluacion, obtenerEvaluacion } from '../../estudiante/services/estudianteService.js'

export default function HeteroevaluacionClase() {
  const { idAsignacion } = useParams()
  const { usuario } = useSesion()
  const cargar = useCallback(() => obtenerEvaluacion(idAsignacion), [idAsignacion])
  const { datos, error, cargando, recargar } = useFetch(cargar, [cargar])
  const [enviada, setEnviada] = useState(false)
  const enviar = useCallback(
    async (envio) => {
      await enviarEvaluacion(idAsignacion, envio)
      // Copia solo en este navegador: el servidor no sabe quién respondió qué.
      guardarResumen(usuario.idUsuario, idAsignacion, envio)
      setEnviada(true)
    },
    [idAsignacion, usuario.idUsuario],
  )
  const yaEnviada = enviada || datos?.estado === 'ENVIADA'
  // eslint-disable-next-line react-hooks/exhaustive-deps -- se vuelve a leer al quedar enviada
  const copia = useMemo(() => (yaEnviada ? leerResumen(usuario.idUsuario, idAsignacion) : null), [yaEnviada, idAsignacion])
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
        {datos && yaEnviada && <ResumenEstudiante formulario={datos} copia={copia} />}
        {datos && !yaEnviada && <FormularioEstudiante formulario={datos} onEnviar={enviar} />}
      </div>
    </>
  )
}
