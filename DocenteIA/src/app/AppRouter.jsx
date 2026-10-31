import { GoogleOAuthProvider } from '@react-oauth/google'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import CambiarContrasena from '../features/auth/pages/CambiarContrasena.jsx'
import Login from '../features/auth/pages/Login.jsx'
import { SesionProvider } from '../features/auth/SesionProvider.jsx'
import { useSesion } from '../features/auth/sesionContext.js'
import CalificarDocente from '../features/calificaciones/pages/CalificarDocente.jsx'
import Agenda from '../features/agenda/pages/Agenda.jsx'
import MiCita from '../features/agenda/pages/MiCita.jsx'
import Analisis from '../features/analisis/pages/Analisis.jsx'
import Docentes from '../features/docentes/pages/Docentes.jsx'
import ExpedienteDocente from '../features/expedientes/pages/ExpedienteDocente.jsx'
import MiEvaluacion from '../features/expedientes/pages/MiEvaluacion.jsx'
import Facultades from '../features/facultades/pages/Facultades.jsx'
import Importaciones from '../features/importaciones/pages/Importaciones.jsx'
import Inicio from '../features/kpis/pages/Inicio.jsx'
import Periodos from '../features/periodos/pages/Periodos.jsx'
import Ponderaciones from '../features/ponderaciones/pages/Ponderaciones.jsx'
import Programas from '../features/programas/pages/Programas.jsx'
import FormularioUsuario from '../features/usuarios/pages/FormularioUsuario.jsx'
import Usuarios from '../features/usuarios/pages/Usuarios.jsx'
import { AppLayout } from '../shared/components/layout/AppLayout/AppLayout.jsx'
import { ROLES } from '../shared/constants/dominio.js'
import { PaginaNoEncontrada } from './PaginasSistema.jsx'
import { ProtectedRoute } from './ProtectedRoute.jsx'
import { RUTA_CAMBIO_CONTRASENA, rutaInicioDelRol } from './rutasPorRol.js'

const GESTION = [ROLES.COORDINADOR, ROLES.VICERRECTOR]

const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim() ?? ''

function RedirigirAlInicio() {
  const { usuario, validando } = useSesion()
  if (validando) return null
  return <Navigate to={usuario ? rutaInicioDelRol(usuario.rol) : '/login'} replace />
}

function RutasDocentia() {
  return (
    <BrowserRouter>
      <SesionProvider>
        <Routes>
          <Route path="/" element={<RedirigirAlInicio />} />
          <Route path="/login" element={<Login />} />

          <Route element={<ProtectedRoute />}>
            {/* Fuera del layout: con la contraseña temporal el resto de la API responde 403. */}
            <Route path={RUTA_CAMBIO_CONTRASENA} element={<CambiarContrasena />} />

            <Route element={<AppLayout />}>
              <Route element={<ProtectedRoute rolesPermitidos={[ROLES.DOCENTE]} />}>
                <Route path="/mi-evaluacion" element={<MiEvaluacion />} />
                <Route path="/mi-cita" element={<MiCita />} />
              </Route>

              <Route element={<ProtectedRoute rolesPermitidos={GESTION} />}>
                <Route path="/inicio" element={<Inicio />} />
                <Route path="/importaciones" element={<Importaciones />} />
                <Route path="/analisis" element={<Analisis />} />
                <Route path="/docentes" element={<Docentes />} />
                <Route path="/docentes/:idDocente" element={<ExpedienteDocente />} />
                <Route path="/docentes/:idDocente/calificar" element={<CalificarDocente />} />
              </Route>

              <Route element={<ProtectedRoute rolesPermitidos={[ROLES.COORDINADOR]} />}>
                <Route path="/agenda" element={<Agenda />} />
              </Route>

              <Route element={<ProtectedRoute rolesPermitidos={[ROLES.VICERRECTOR]} />}>
                <Route path="/periodos" element={<Periodos />} />
                <Route path="/ponderaciones" element={<Ponderaciones />} />
                <Route path="/facultades" element={<Facultades />} />
                <Route path="/programas" element={<Programas />} />
                {/* Crear y editar se abren como modal sobre la lista */}
                <Route path="/usuarios" element={<Usuarios />}>
                  <Route path="nuevo" element={<FormularioUsuario />} />
                  <Route path=":idUsuario" element={<FormularioUsuario />} />
                </Route>
              </Route>

              <Route path="*" element={<PaginaNoEncontrada />} />
            </Route>
          </Route>
        </Routes>
      </SesionProvider>
    </BrowserRouter>
  )
}

export function AppRouter() {
  if (googleClientId) {
    return (
      <GoogleOAuthProvider clientId={googleClientId}>
        <RutasDocentia />
      </GoogleOAuthProvider>
    )
  }
  return <RutasDocentia />
}
