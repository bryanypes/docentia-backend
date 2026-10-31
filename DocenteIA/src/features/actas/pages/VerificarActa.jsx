import { useCallback } from 'react'
import { useParams } from 'react-router-dom'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Cargando } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFechaHora } from '../../../shared/utils/formatters.js'
import { verificarActa } from '../services/actasService.js'

/** Página pública: confirma que un código corresponde a un acta. No muestra notas. */
export default function VerificarActa() {
  const { codigo } = useParams()
  const cargar = useCallback(() => verificarActa(codigo), [codigo])
  const { datos, error, cargando } = useFetch(cargar, [cargar])

  return (
    <main className="pagina">
      <h1>Verificación de acta</h1>
      {cargando && <Cargando texto="Verificando" bloques={1} />}
      {error && (
        <Aviso tipo="error" titulo={error.status === 404 ? 'El código no corresponde a ningún acta' : error.message} />
      )}
      {datos && (
        <Aviso
          tipo={datos.estado === 'FIRMADA' ? 'exito' : 'alerta'}
          titulo={datos.estado === 'FIRMADA' ? 'Acta válida' : 'Esta versión del acta fue anulada'}
        >
          <p>
            {datos.nombreDocente} · Periodo {datos.periodo} · Versión {datos.version}
          </p>
          <p>Firmada el {formatearFechaHora(datos.fechaFirma)}</p>
          <p className="acta__sello">SHA-256 {datos.hashContenido}</p>
        </Aviso>
      )}
    </main>
  )
}
