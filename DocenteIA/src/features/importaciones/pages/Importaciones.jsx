import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { useSesion } from '../../auth/sesionContext.js'
import { HistorialImportaciones } from '../components/HistorialImportaciones.jsx'
import { listarImportaciones } from '../services/importacionesService.js'
import './Importaciones.css'

/**
 * Historial de cargas CSV anteriores. La subida se cortó: la fuente ESTUDIANTES llega del formulario
 * HETERO. La ruta se conserva por si alguien tiene el enlace; ya no está en el menú.
 */
export default function Importaciones() {
  const { usuario } = useSesion()
  const esCoordinador = usuario.rol === ROLES.COORDINADOR
  const importaciones = useFetch(() => listarImportaciones(), [])

  return (
    <>
      <Encabezado
        miga={`${esCoordinador ? 'Coordinación' : 'Vicerrectoría'} · Historial de cargas`}
        titulo="Historial de evaluación de estudiantes"
        subtitulo="La evaluación de estudiantes se responde en el formulario. Aquí solo queda el historial de cargas anteriores."
      />

      <div className="pagina">
        <Aviso tipo="info" titulo="La carga de CSV ya no está disponible">
          Los estudiantes califican a sus profesores en Evaluación a profesor. Un archivo nuevo se rechaza.
        </Aviso>

        <HistorialImportaciones
          importaciones={importaciones}
          mostrarUsuario={!esCoordinador}
          descripcion={
            esCoordinador
              ? 'Tus cargas anteriores, de la más reciente a la más antigua.'
              : 'Todas las cargas anteriores de la universidad.'
          }
        />
      </div>
    </>
  )
}
