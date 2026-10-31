import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { useSesion } from '../../auth/sesionContext.js'
import { CargaCsv } from '../components/CargaCsv.jsx'
import { HistorialImportaciones } from '../components/HistorialImportaciones.jsx'
import { PanelPlantillas } from '../components/PanelPlantillas.jsx'
import { listarImportaciones, listarPlantillas } from '../services/importacionesService.js'
import './Importaciones.css'

/**
 * Coordinación carga el CSV de evaluación de estudiantes de su facultad y administra sus plantillas.
 * Vicerrectoría solo consulta el historial de todas las cargas.
 */
export default function Importaciones() {
  const { usuario } = useSesion()
  const esCoordinador = usuario.rol === ROLES.COORDINADOR

  const importaciones = useFetch(() => listarImportaciones(), [])
  const plantillas = useFetch(() => (esCoordinador ? listarPlantillas() : Promise.resolve([])), [esCoordinador])

  return (
    <>
      <Encabezado
        miga={`${esCoordinador ? 'Coordinación' : 'Vicerrectoría'} · Evaluación de estudiantes`}
        titulo={esCoordinador ? 'Carga de evaluación de estudiantes' : 'Cargas de evaluación de estudiantes'}
        subtitulo={
          esCoordinador
            ? 'Sube el CSV de la encuesta, revisa el reporte y luego impórtalo. Si una sola fila tiene error, no se guarda nada.'
            : 'Historial de los archivos que cargó cada coordinación, incluidos los rechazados.'
        }
      />

      <div className="pagina">
        {esCoordinador && <CargaCsv plantillas={plantillas} alImportar={importaciones.recargar} />}

        {esCoordinador && <PanelPlantillas plantillas={plantillas} />}

        <HistorialImportaciones
          importaciones={importaciones}
          mostrarUsuario={!esCoordinador}
          descripcion={esCoordinador ? 'Tus cargas, de la más reciente a la más antigua.' : 'Todas las cargas de la universidad.'}
        />
      </div>
    </>
  )
}
