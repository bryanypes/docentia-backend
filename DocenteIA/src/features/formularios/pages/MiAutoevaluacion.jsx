import { RefreshCw } from 'lucide-react'
import { useCallback } from 'react'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { FormularioInstrumento } from '../components/FormularioInstrumento.jsx'
import {
  enviarAutoevaluacion,
  guardarAutoevaluacion,
  obtenerAutoevaluacion,
} from '../services/formulariosService.js'

export default function MiAutoevaluacion() {
  const formulario = useFetch(obtenerAutoevaluacion)
  const { datos, error, cargando, recargar } = formulario
  const enviar = useCallback(async () => {
    await enviarAutoevaluacion()
    recargar()
  }, [recargar])

  return (
    <>
      <Encabezado
        miga="Docente · Mi autoevaluación"
        titulo="Mi autoevaluación"
        subtitulo="Catorce ítems del Anexo 1 de la Resolución 074. Pesa el 20 % de tu resultado por defecto."
      />
      <div className="pagina">
        {cargando && !datos && <Cargando texto="Cargando tu autoevaluación" bloques={2} />}
        {error && (
          <EstadoVista
            tipo={error.status === 409 ? 'vacio' : 'error'}
            titulo={error.status === 409 ? 'No hay un periodo activo' : 'No fue posible cargar el formulario'}
            mensaje={error.message}
          >
            <Boton icono={RefreshCw} onClick={recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}
        {datos && (
          <FormularioInstrumento
            key={`${datos.periodo.idPeriodo}-${datos.estado}`}
            formulario={datos}
            onGuardar={guardarAutoevaluacion}
            onEnviar={enviar}
          />
        )}
      </div>
    </>
  )
}
