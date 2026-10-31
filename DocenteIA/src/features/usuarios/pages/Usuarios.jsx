import { Pencil, Power, PowerOff, RefreshCw, Search, UserPlus } from 'lucide-react'
import { useCallback, useMemo, useState } from 'react'
import { Link, Outlet, useNavigate } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { MensajeNavegacion } from '../../../shared/components/ui/MensajeNavegacion/MensajeNavegacion.jsx'
import { ETIQUETA_ROL, ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { iniciales, normalizarTexto } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { cambiarEstadoUsuario, listarUsuarios } from '../services/usuariosService.js'
import './Usuarios.css'

const TODOS = ''

/** Facultad y programa al que está vinculado el usuario según su rol. */
function vinculo(usuario) {
  if (usuario.rol === ROLES.VICERRECTOR) return { principal: 'Toda la universidad' }
  if (usuario.rol === ROLES.DOCENTE) return { principal: usuario.nombrePrograma, secundario: usuario.nombreFacultad }
  return { principal: usuario.nombreFacultad }
}

export default function Usuarios() {
  const navegar = useNavigate()
  const { usuario: sesion } = useSesion()
  const [rol, setRol] = useState(TODOS)
  const [busqueda, setBusqueda] = useState('')
  // Usuarios cuyo estado cambió aquí: se muestran con la respuesta del backend sin recargar la lista.
  const [actualizados, setActualizados] = useState({})
  const [cambiando, setCambiando] = useState(null)
  const [resultado, setResultado] = useState(null)

  const cargar = useCallback(() => listarUsuarios(rol || null), [rol])
  const usuarios = useFetch(cargar, [cargar])

  const lista = useMemo(
    () => (usuarios.datos ?? []).map((u) => actualizados[u.idUsuario] ?? u),
    [usuarios.datos, actualizados],
  )
  const filas = useMemo(() => {
    const termino = normalizarTexto(busqueda.trim())
    if (!termino) return lista
    return lista.filter((u) =>
      normalizarTexto(`${u.nombreCompleto} ${u.correo} ${u.nombrePrograma ?? ''} ${u.nombreFacultad ?? ''}`).includes(termino),
    )
  }, [lista, busqueda])

  async function alternarEstado(u) {
    const activar = !u.esActivo
    if (
      !activar &&
      !window.confirm(
        `¿Desactivar la cuenta de ${u.nombreCompleto}? No podrá ingresar y su sesión abierta se cerrará. Sus calificaciones e historial se conservan.`,
      )
    ) {
      return
    }
    setCambiando(u.idUsuario)
    setResultado(null)
    try {
      const actualizado = await cambiarEstadoUsuario(u.idUsuario, activar)
      setActualizados((a) => ({ ...a, [actualizado.idUsuario]: actualizado }))
      setResultado({
        tipo: 'exito',
        texto: `La cuenta de ${actualizado.nombreCompleto} quedó ${actualizado.esActivo ? 'activa' : 'inactiva'}.`,
      })
    } catch (error) {
      setResultado({ tipo: 'error', texto: error.message })
    } finally {
      setCambiando(null)
    }
  }

  return (
    <>
      <Encabezado
        miga="Vicerrectoría · Usuarios"
        titulo="Usuarios y roles"
        subtitulo="Cada persona tiene un solo rol. La coordinación queda vinculada a una facultad y el docente a un programa."
      >
        <Boton icono={UserPlus} onClick={() => navegar('/usuarios/nuevo', { state: { conservarFoco: true } })}>
          Nuevo usuario
        </Boton>
      </Encabezado>

      <div className="pagina">
        <MensajeNavegacion />
        <div aria-live="polite">{resultado && <Aviso tipo={resultado.tipo} titulo={resultado.texto} />}</div>

        <div className="usuarios__filtros">
          <label className="usuarios__buscador">
            <Search size={16} aria-hidden="true" />
            <span className="solo-lectores">Buscar usuario</span>
            <input
              type="search"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              placeholder="Buscar por nombre, correo, programa o facultad"
            />
          </label>
          <label className="usuarios__filtro">
            <span className="usuarios__filtro-etiqueta">Rol</span>
            <select className="campo__control usuarios__selector" value={rol} onChange={(e) => setRol(e.target.value)}>
              <option value={TODOS}>Todos los roles</option>
              {Object.values(ROLES).map((r) => (
                <option key={r} value={r}>
                  {ETIQUETA_ROL[r]}
                </option>
              ))}
            </select>
          </label>
          <span className="usuarios__conteo" aria-live="polite">
            {filas.length} de {lista.length}
          </span>
        </div>

        {usuarios.cargando && <Cargando texto="Cargando usuarios" bloques={1} />}

        {usuarios.error && (
          <EstadoVista tipo="error" titulo="No fue posible cargar los usuarios" mensaje={usuarios.error.message}>
            <Boton icono={RefreshCw} onClick={usuarios.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}

        {!usuarios.cargando && !usuarios.error && filas.length === 0 && (
          <EstadoVista
            titulo={lista.length === 0 ? 'No hay usuarios con este rol' : 'Ningún usuario coincide con la búsqueda'}
            mensaje="Prueba con otro rol o limpia la búsqueda."
          />
        )}

        {!usuarios.cargando && !usuarios.error && filas.length > 0 && (
          <div className="tabla-contenedor">
            <table className="tabla usuarios__tabla">
              <caption className="solo-lectores">Usuarios</caption>
              <thead>
                <tr>
                  <th scope="col">Usuario</th>
                  <th scope="col">Rol</th>
                  <th scope="col">Vínculo</th>
                  <th scope="col">Estado</th>
                  <th scope="col">
                    <span className="solo-lectores">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {filas.map((u) => {
                  const { principal, secundario } = vinculo(u)
                  const esPropio = u.idUsuario === sesion.idUsuario
                  return (
                    <tr key={u.idUsuario} className={u.esActivo ? undefined : 'usuarios__fila--inactiva'}>
                      <td>
                        <div className="usuarios__persona">
                          <span className="usuarios__avatar" aria-hidden="true">
                            {iniciales(u.nombreCompleto)}
                          </span>
                          <span>
                            <span className="tabla__fuerte">
                              {u.nombreCompleto}
                              {esPropio && <span className="usuarios__tu"> (tú)</span>}
                            </span>
                            <span className="tabla__tenue">{u.correo}</span>
                          </span>
                        </div>
                      </td>
                      <td>
                        <span className="pildora pildora--info">{ETIQUETA_ROL[u.rol]}</span>
                      </td>
                      <td>
                        {principal ?? '—'}
                        {secundario && <span className="tabla__tenue">{secundario}</span>}
                      </td>
                      <td>
                        <div className="usuarios__estado">
                          <span className={`pildora pildora--${u.esActivo ? 'exito' : 'neutro'}`}>
                            {u.esActivo ? 'Activo' : 'Inactivo'}
                          </span>
                          {u.esActivo && u.debeCambiarContrasena && (
                            <span className="pildora pildora--alerta" title="Aún no cambia la contraseña que se le asignó">
                              Contraseña temporal
                            </span>
                          )}
                        </div>
                      </td>
                      <td>
                        <div className="usuarios__acciones">
                          <Link
                            to={`/usuarios/${u.idUsuario}`}
                            state={{ conservarFoco: true }}
                            className="usuarios__accion"
                            aria-label={`Editar a ${u.nombreCompleto}`}
                          >
                            <Pencil size={15} aria-hidden="true" />
                            Editar
                          </Link>
                          {!esPropio && (
                            <button
                              type="button"
                              className={`usuarios__accion${u.esActivo ? ' usuarios__accion--peligro' : ''}`}
                              onClick={() => alternarEstado(u)}
                              disabled={cambiando === u.idUsuario}
                              aria-label={`${u.esActivo ? 'Desactivar' : 'Activar'} la cuenta de ${u.nombreCompleto}`}
                            >
                              {u.esActivo ? <PowerOff size={15} aria-hidden="true" /> : <Power size={15} aria-hidden="true" />}
                              {cambiando === u.idUsuario ? 'Guardando…' : u.esActivo ? 'Desactivar' : 'Activar'}
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal de crear o editar (rutas hijas); al guardar recarga la lista */}
      <Outlet context={{ alGuardar: usuarios.recargar }} />
    </>
  )
}
