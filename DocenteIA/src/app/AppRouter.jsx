import { GoogleOAuthProvider } from '@react-oauth/google'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import CambiarContrasena from '../features/auth/pages/CambiarContrasena.jsx'
import MiPerfil from '../features/perfil/pages/MiPerfil.jsx'
import Login from '../features/auth/pages/Login.jsx'
import { SesionProvider } from '../features/auth/SesionProvider.jsx'
import { useSesion } from '../features/auth/sesionContext.js'
import AccionesSugeridas from '../features/actas/pages/AccionesSugeridas.jsx'
import VerificarActa from '../features/actas/pages/VerificarActa.jsx'
import MiActa from '../features/actas/pages/MiActa.jsx'
import CoevaluacionDocente from '../features/formularios/pages/CoevaluacionDocente.jsx'
import Coevaluaciones from '../features/formularios/pages/Coevaluaciones.jsx'
import EvaluacionProfesores from '../features/formularios/pages/EvaluacionProfesores.jsx'
import HeteroevaluacionClase from '../features/formularios/pages/HeteroevaluacionClase.jsx'
import MiAutoevaluacion from '../features/formularios/pages/MiAutoevaluacion.jsx'
import Matricula from '../features/heteroevaluacion/pages/Matricula.jsx'
import Notificaciones from '../features/notificaciones/pages/Notificaciones.jsx'
import PanelProgramas from '../features/kpis/pages/PanelProgramas.jsx'
import Agenda from '../features/agenda/pages/Agenda.jsx'
import MiCita from '../features/agenda/pages/MiCita.jsx'
import Analisis from '../features/analisis/pages/Analisis.jsx'
import Docentes from '../features/docentes/pages/Docentes.jsx'
import Estadisticas from '../features/estadisticas/pages/Estadisticas.jsx'
import ExpedienteDocente from '../features/expedientes/pages/ExpedienteDocente.jsx'
import MiEvaluacion from '../features/expedientes/pages/MiEvaluacion.jsx'
import Facultades from '../features/facultades/pages/Facultades.jsx'
import Importaciones from '../features/importaciones/pages/Importaciones.jsx'
import Periodos from '../features/periodos/pages/Periodos.jsx'
import Ponderaciones from '../features/ponderaciones/pages/Ponderaciones.jsx'
import FormularioUsuario from '../features/usuarios/pages/FormularioUsuario.jsx'
import Usuarios from '../features/usuarios/pages/Usuarios.jsx'
import { AppLayout } from '../shared/components/layout/AppLayout/AppLayout.jsx'
import { AvisosProvider } from '../shared/components/ui/Avisos/AvisosProvider.jsx'
import { ROLES } from '../shared/constants/dominio.js'
import { FUNCIONES } from '../shared/constants/funciones.js'
import InicioPorRol from './InicioPorRol.jsx'
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
        <AvisosProvider>
        <Routes>
          <Route path="/" element={<RedirigirAlInicio />} />
          <Route path="/login" element={<Login />} />
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
                <Route
                  path="/importaciones"
                  element={FUNCIONES.cargaCsvEstudiantes ? <Importaciones /> : <Navigate to="/inicio" replace />}
                />
                <Route path="/analisis" element={<Analisis />} />
                <Route path="/estadisticas" element={<Estadisticas />} />
                <Route path="/docentes" element={<Docentes />} />
                <Route path="/docentes/:idDocente" element={<ExpedienteDocente />} />
                <Route path="/acciones-sugeridas" element={<AccionesSugeridas />} />
                <Route
                  path="/matricula"
                  element={FUNCIONES.coberturaEstudiantes ? <Matricula /> : <Navigate to="/inicio" replace />}
                />
                <Route path="/panel-programas" element={<PanelProgramas />} />
              </Route>

              <Route element={<ProtectedRoute rolesPermitidos={[ROLES.COORDINADOR]} />}>
                <Route path="/agenda" element={<Agenda />} />
                <Route path="/coevaluaciones" element={<Coevaluaciones />} />
                <Route path="/coevaluaciones/:idDocente" element={<CoevaluacionDocente />} />
              </Route>

              <Route element={<ProtectedRoute rolesPermitidos={[ROLES.VICERRECTOR]} />}>
                <Route path="/periodos" element={<Periodos />} />
                <Route path="/auditoria" element={<Navigate to="/usuarios?vista=actividad" replace />} />
                <Route path="/ponderaciones" element={<Ponderaciones />} />
                <Route path="/facultades" element={<Facultades />} />
                <Route path="/programas" element={<Navigate to="/facultades" replace />} />
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
