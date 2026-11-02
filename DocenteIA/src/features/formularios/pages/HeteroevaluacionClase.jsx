import { ChevronLeft, RefreshCw } from 'lucide-react'
import { useCallback } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { FormularioInstrumento } from '../components/FormularioInstrumento.jsx'
import {
  enviarHeteroevaluacion,
  guardarHeteroevaluacion,
  obtenerHeteroevaluacion,
} from '../services/formulariosService.js'

function subtituloDe(datos) {
  if (!datos) return 'Once ítems del Anexo 1. El envío es irreversible.'
  const curso = datos.nombreCurso ? `${datos.nombreCurso} · ` : ''
  const grupo = datos.grupo ? `Grupo ${datos.grupo}. ` : ''
  return `${curso}${grupo}Once ítems del Anexo 1. El envío es irreversible.`
}

export default function HeteroevaluacionClase() {
  const { idClase, idDocente } = useParams()
  const cargar = useCallback(() => obtenerHeteroevaluacion(idClase, idDocente), [idClase, idDocente])
  const { datos, error, cargando, recargar } = useFetch(cargar, [cargar])
  const guardar = useCallback(
    (borrador) => guardarHeteroevaluacion(idClase, idDocente, borrador),
    [idClase, idDocente],
  )
  const enviar = useCallback(async () => {
    await enviarHeteroevaluacion(idClase, idDocente)
    recargar()
  }, [idClase, idDocente, recargar])
  const noEncontrado = error?.status === 404
  const etapaCerrada = error?.status === 409

  return (
    <>
      <Encabezado
        miga="Estudiante · Evaluación a profesor"
        titulo={datos ? `Evaluación de ${datos.nombreDocente}` : 'Evaluación a profesor'}
        subtitulo={subtituloDe(datos)}
      />
      <div className="pagina">
        <Link to="/evaluacion-profesores" className="enlace-volver">
          <ChevronLeft size={16} aria-hidden="true" />
          Volver a tus profesores
        </Link>
        {cargando && !datos && <Cargando texto="Cargando el formulario" bloques={2} />}
        {error && (
          <EstadoVista
            tipo={noEncontrado || etapaCerrada ? 'vacio' : 'error'}
            titulo={
              noEncontrado
                ? 'Formulario no encontrado'
                : etapaCerrada
                  ? 'No se puede responder ahora'
                  : 'No fue posible cargar el formulario'
            }
            mensaje={
              noEncontrado
                ? 'Esa clase no está entre tus matrículas de este periodo.'
                : error.message
            }
          >
            {!noEncontrado && (
              <Boton icono={RefreshCw} onClick={recargar}>
                Intentar de nuevo
              </Boton>
            )}
          </EstadoVista>
        )}
        {datos && (
          <FormularioInstrumento
            key={`${datos.idClase}-${datos.idDocente}-${datos.estado}`}
            formulario={datos}
            onGuardar={guardar}
            onEnviar={enviar}
          />
        )}
      </div>
    </>
  )
}
