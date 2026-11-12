import { GoogleOAuthProvider } from '@react-oauth/google'
import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import Login from '../features/auth/pages/Login.jsx'
import { SesionProvider } from '../features/auth/SesionProvider.jsx'
import { useSesion } from '../features/auth/sesionContext.js'
import { AppLayout } from '../shared/components/layout/AppLayout/AppLayout.jsx'
import { AvisosProvider } from '../shared/components/ui/Avisos/AvisosProvider.jsx'
import { ROLES } from '../shared/constants/dominio.js'
import InicioPorRol from './InicioPorRol.jsx'
import { PaginaNoEncontrada } from './PaginasSistema.jsx'
import { ProtectedRoute } from './ProtectedRoute.jsx'
import RecuperarContrasena from '../features/auth/pages/RecuperarContrasena.jsx'
import Presentacion from '../features/presentacion/Presentacion.jsx'
import { RUTA_CAMBIO_CONTRASENA, rutaInicioDelRol } from './rutasPorRol.js'

// Cada pantalla se descarga al abrirla: la carga inicial solo trae el login y el marco.
const CambiarContrasena = lazy(() => import('../features/auth/pages/CambiarContrasena.jsx'))
const MiPerfil = lazy(() => import('../features/perfil/pages/MiPerfil.jsx'))
const AccionesSugeridas = lazy(() => import('../features/actas/pages/AccionesSugeridas.jsx'))
const VerificarActa = lazy(() => import('../features/actas/pages/VerificarActa.jsx'))
const MiActa = lazy(() => import('../features/actas/pages/MiActa.jsx'))
const CoevaluacionDocente = lazy(() => import('../features/formularios/pages/CoevaluacionDocente.jsx'))
const Coevaluaciones = lazy(() => import('../features/formularios/pages/Coevaluaciones.jsx'))
const EvaluacionProfesores = lazy(() => import('../features/formularios/pages/EvaluacionProfesores.jsx'))
const HeteroevaluacionClase = lazy(() => import('../features/formularios/pages/HeteroevaluacionClase.jsx'))
const MiAutoevaluacion = lazy(() => import('../features/formularios/pages/MiAutoevaluacion.jsx'))
const Notificaciones = lazy(() => import('../features/notificaciones/pages/Notificaciones.jsx'))
const PanelProgramas = lazy(() => import('../features/kpis/pages/PanelProgramas.jsx'))
const Agenda = lazy(() => import('../features/agenda/pages/Agenda.jsx'))
const MiCita = lazy(() => import('../features/agenda/pages/MiCita.jsx'))
const Analisis = lazy(() => import('../features/analisis/pages/Analisis.jsx'))
const Docentes = lazy(() => import('../features/docentes/pages/Docentes.jsx'))
const Estadisticas = lazy(() => import('../features/estadisticas/pages/Estadisticas.jsx'))
const ExpedienteDocente = lazy(() => import('../features/expedientes/pages/ExpedienteDocente.jsx'))
const MiEvaluacion = lazy(() => import('../features/expedientes/pages/MiEvaluacion.jsx'))
const Periodos = lazy(() => import('../features/periodos/pages/Periodos.jsx'))
const Ponderaciones = lazy(() => import('../features/ponderaciones/pages/Ponderaciones.jsx'))
const Programas = lazy(() => import('../features/programas/pages/Programas.jsx'))
const FormularioUsuario = lazy(() => import('../features/usuarios/pages/FormularioUsuario.jsx'))
const Usuarios = lazy(() => import('../features/usuarios/pages/Usuarios.jsx'))

const GESTION = [ROLES.COORDINADOR, ROLES.VICERRECTOR]

const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim() ?? ''

/** En la raíz: con sesión va al inicio de su rol; sin sesión, la presentación pública. */
function RedirigirAlInicio() {
  const { usuario, validando } = useSesion()
  if (validando) return null
  return usuario ? <Navigate to={rutaInicioDelRol(usuario.rol)} replace /> : <Presentacion />
}

function RutasDocentia() {
  return (
    <BrowserRouter>
      <SesionProvider>
        <AvisosProvider>
        <Suspense fallback={null}>
        <Routes>
          <Route path="/" element={<RedirigirAlInicio />} />
          <Route path="/login" element={<Login />} />
          <Route path="/recuperar-contrasena" element={<RecuperarContrasena />} />
          <Route path="/restablecer-contrasena" element={<RecuperarContrasena />} />
          <Route path="/verificar-acta/:codigo" element={<VerificarActa />} />

          <Route element={<ProtectedRoute />}>
            {/* Fuera del layout: con la contraseña temporal el resto de la API responde 403. */}
            <Route path={RUTA_CAMBIO_CONTRASENA} element={<CambiarContrasena />} />

            <Route element={<AppLayout />}>
              <Route path="/inicio" element={<InicioPorRol />} />
              <Route path="/notificaciones" element={<Notificaciones />} />
              <Route path="/perfil" element={<MiPerfil />} />
              <Route element={<ProtectedRoute rolesPermitidos={[ROLES.ESTUDIANTE]} />}>
                <Route path="/evaluacion-profesores" element={<EvaluacionProfesores />} />
                <Route
                  path="/evaluacion-profesores/:idAsignacion"
                  element={<HeteroevaluacionClase />}
                />
              </Route>

              <Route element={<ProtectedRoute rolesPermitidos={[ROLES.DOCENTE]} />}>
                <Route path="/mi-evaluacion" element={<MiEvaluacion />} />
                <Route path="/mi-cita" element={<MiCita />} />
                <Route path="/mi-autoevaluacion" element={<MiAutoevaluacion />} />
                <Route path="/mi-acta" element={<MiActa />} />
              </Route>

              <Route element={<ProtectedRoute rolesPermitidos={GESTION} />}>
                <Route path="/analisis" element={<Analisis />} />
                <Route path="/estadisticas" element={<Estadisticas />} />
                <Route path="/docentes" element={<Docentes />} />
                <Route path="/docentes/:idDocente" element={<ExpedienteDocente />} />
                <Route path="/acciones-sugeridas" element={<AccionesSugeridas />} />
              </Route>

              <Route element={<ProtectedRoute rolesPermitidos={[ROLES.COORDINADOR]} />}>
                <Route path="/agenda" element={<Agenda />} />
                <Route path="/coevaluaciones" element={<Coevaluaciones />} />
                <Route path="/coevaluaciones/:idDocente" element={<CoevaluacionDocente />} />
              </Route>

              <Route element={<ProtectedRoute rolesPermitidos={[ROLES.COORDINADOR, ROLES.VICERRECTOR, ROLES.VISUALIZADOR]} />}>
                <Route path="/panel-programas" element={<PanelProgramas />} />
              </Route>

              <Route element={<ProtectedRoute rolesPermitidos={[ROLES.VICERRECTOR]} />}>
              </Route>

              {/* Vicerrectoría decide fechas y lee la configuración; administración la gestiona. */}
              <Route element={<ProtectedRoute rolesPermitidos={[ROLES.VICERRECTOR, ROLES.ADMINISTRADOR]} />}>
                <Route path="/periodos" element={<Periodos />} />
                <Route path="/ponderaciones" element={<Ponderaciones />} />
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
        </Suspense>
        </AvisosProvider>
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
