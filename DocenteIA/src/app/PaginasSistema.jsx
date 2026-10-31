import { Link } from 'react-router-dom'
import { useSesion } from '../features/auth/sesionContext.js'
import { EstadoVista } from '../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { useTituloDocumento } from '../shared/hooks/useTituloDocumento.js'
import { rutaInicioDelRol } from './rutasPorRol.js'

function EnlaceInicio() {
  const { usuario } = useSesion()
  return <Link to={usuario ? rutaInicioDelRol(usuario.rol) : '/login'}>Ir al inicio</Link>
}

export function PaginaSinAcceso() {
  useTituloDocumento('Sin acceso')
  return (
    <div className="pagina">
      <EstadoVista tipo="error" titulo="Sin acceso" mensaje="Tu rol no tiene permiso para ver esta sección.">
        <EnlaceInicio />
      </EstadoVista>
    </div>
  )
}

export function PaginaNoEncontrada() {
  useTituloDocumento('Página no encontrada')
  return (
    <div className="pagina">
      <EstadoVista titulo="Página no encontrada" mensaje="La dirección no existe o cambió.">
        <EnlaceInicio />
      </EstadoVista>
    </div>
  )
}
