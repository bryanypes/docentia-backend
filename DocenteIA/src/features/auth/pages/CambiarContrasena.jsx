import { Navigate, useNavigate } from 'react-router-dom'
import { rutaInicioDelRol } from '../../../app/rutasPorRol.js'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { useTituloDocumento } from '../../../shared/hooks/useTituloDocumento.js'
import { FormularioContrasena } from '../components/FormularioContrasena.jsx'
import { useCambioContrasena } from '../components/useCambioContrasena.js'
import { PantallaAcceso } from '../components/PantallaAcceso.jsx'
import { useSesion } from '../sesionContext.js'

/**
 * Cambio obligatorio mientras la contraseña sea temporal (la asignó la vicerrectoría): no hay forma
 * de salir de aquí salvo cerrando sesión. El cambio voluntario vive en el modal de Mi perfil.
 */
export default function CambiarContrasena() {
  const { usuario } = useSesion()
  if (!usuario.debeCambiarContrasena) {
    return <Navigate to="/perfil" replace state={{ cambiarContrasena: true }} />
  }
  return <CambioObligatorio />
}

function CambioObligatorio() {
  const { usuario, reemplazarSesion, cerrarSesion } = useSesion()
  const navegar = useNavigate()
  useTituloDocumento('Crea tu contraseña')
  const cambio = useCambioContrasena((sesion) => {
    reemplazarSesion(sesion)
    navegar(rutaInicioDelRol(sesion.usuario.rol), {
      replace: true,
      state: { mensaje: 'Tu contraseña quedó actualizada.' },
    })
  })

  return (
    <PantallaAcceso avatar={cambio.avatar} estadoAvatar={cambio.listo && !cambio.enviando ? 'Contraseña lista para guardar' : null}>
      <FormularioContrasena
        cambio={cambio}
        obligatorio
        encabezado={
          <div className="cambio-contrasena__encabezado">
            <h1>Crea tu contraseña</h1>
            <p>
              Hola, {usuario.nombreCompleto.split(' ')[0]}. La contraseña con la que ingresaste es temporal: elige una
              propia para continuar.
            </p>
          </div>
        }
        acciones={
          <Boton variante="secundario" bloque onClick={() => cerrarSesion()} disabled={cambio.enviando}>
            Salir sin cambiarla
          </Boton>
        }
      />
    </PantallaAcceso>
  )
}
