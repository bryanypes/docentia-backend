import { Building2, Power, PowerOff, RefreshCw, Search, UserPlus, UserRound } from 'lucide-react'
import { useCallback, useMemo, useState } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { BotonAccion } from '../../../shared/components/ui/BotonAccion/BotonAccion.jsx'
import { Modal } from '../../../shared/components/ui/Modal/Modal.jsx'
import { Paginacion } from '../../../shared/components/ui/Paginacion/Paginacion.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { MensajeNavegacion } from '../../../shared/components/ui/MensajeNavegacion/MensajeNavegacion.jsx'
import { ETIQUETA_ROL, ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { useFiltrosUrl, usePaginacion } from '../../../shared/hooks/usePaginacion.js'
import { iniciales, normalizarTexto } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { cambiarEstadoUsuario, listarUsuarios } from '../services/usuariosService.js'
import './Usuarios.css'
import { useAvisos } from '../../../shared/components/ui/Avisos/avisosContext.js'

const TODOS = ''
const FILTROS_PREDETERMINADOS = { rol: '', busqueda: '', facultad: '', agrupar: '' }
const INSTITUCIONAL = 'Institucional (sin facultad)'

const AGRUPACIONES = {
  facultad: { etiqueta: 'Facultad', grupo: (u) => u.nombreFacultad ?? INSTITUCIONAL },
  rol: { etiqueta: 'Rol', grupo: (u) => ETIQUETA_ROL[u.rol] },
  programa: {
    etiqueta: 'Programa',
    grupo: (u) =>
      u.nombrePrograma ?? (u.rol === ROLES.COORDINADOR ? `Coordinación · ${u.nombreFacultad}` : INSTITUCIONAL),
  },
}

/** Orden de los roles dentro de cada facultad en el resumen. */
const ROLES_RESUMEN = [ROLES.COORDINADOR, ROLES.DOCENTE, ROLES.ESTUDIANTE]
const NOMBRE_ROL = {
  COORDINADOR: ['coordinador', 'coordinadores'],
  DOCENTE: ['docente', 'docentes'],
  ESTUDIANTE: ['estudiante', 'estudiantes'],
}

/** Cuántos usuarios activos hay de cada rol en cada facultad, para ver dónde está la gente. */
function resumirPorFacultad(usuarios) {
  const porFacultad = new Map()
  for (const u of usuarios) {
    const nombre = u.nombreFacultad ?? INSTITUCIONAL
    const fila = porFacultad.get(nombre) ?? { nombre, total: 0, roles: {} }
    fila.total += 1
    fila.roles[u.rol] = (fila.roles[u.rol] ?? 0) + 1
    porFacultad.set(nombre, fila)
  }
  return [...porFacultad.values()].sort(
    (a, b) => (a.nombre === INSTITUCIONAL) - (b.nombre === INSTITUCIONAL) || a.nombre.localeCompare(b.nombre, 'es'),
  )
}

/** Facultad y programa al que está vinculado el usuario según su rol. */
function vinculo(usuario) {
  if ([ROLES.VICERRECTOR, ROLES.ADMINISTRADOR, ROLES.VISUALIZADOR].includes(usuario.rol)) {
    return { principal: 'Toda la universidad' }
  }
  if (usuario.rol === ROLES.DOCENTE || usuario.rol === ROLES.ESTUDIANTE) {
    return { principal: usuario.nombrePrograma, secundario: usuario.nombreFacultad }
  }
  return { principal: usuario.nombreFacultad }
}

export default function Usuarios() {
  const { confirmar } = useAvisos()
  const navegar = useNavigate()
  const { usuario: sesion } = useSesion()
  const [viendo, setViendo] = useState(null)
  const [filtros, actualizarFiltros] = useFiltrosUrl(FILTROS_PREDETERMINADOS)
  const { rol, busqueda, facultad, agrupar } = filtros
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
  const resumen = useMemo(() => resumirPorFacultad(lista), [lista])
  const filas = useMemo(() => {
    const termino = normalizarTexto(busqueda.trim())
    return lista.filter(
      (u) =>
        (!facultad || (u.nombreFacultad ?? INSTITUCIONAL) === facultad) &&
        (!termino ||
          normalizarTexto(`${u.nombreCompleto} ${u.correo} ${u.nombrePrograma ?? ''} ${u.nombreFacultad ?? ''}`).includes(
            termino,
          )),
    )
  }, [lista, busqueda, facultad])
  const paginacion = usePaginacion(filas)
  // Agrupado se ve todo de una vez, sin paginar, para que cada grupo quede completo.
  const grupos = useMemo(() => {
    const agrupacion = AGRUPACIONES[agrupar]
    if (!agrupacion) return null
    const mapa = new Map()
    for (const u of filas) {
      const nombre = agrupacion.grupo(u)
      mapa.set(nombre, [...(mapa.get(nombre) ?? []), u])
    }
    return [...mapa.entries()].sort(([a], [b]) => a.localeCompare(b, 'es'))
  }, [filas, agrupar])

  async function alternarEstado(u) {
    const activar = !u.esActivo
    if (
      !activar &&
      !(await confirmar({
        tipo: 'peligro',
        titulo: `¿Desactivar la cuenta de ${u.nombreCompleto}?`,
        mensaje: 'No podrá ingresar y su sesión abierta se cerrará. Sus calificaciones e historial se conservan.',
        textoConfirmar: 'Desactivar',
      }))
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

  const propsFila = { sesion, cambiando, onVer: setViendo, onAlternar: alternarEstado }

  return (
    <>
      <Encabezado
        miga="Administración · Usuarios"
        titulo="Usuarios y roles"
        subtitulo="Cada persona tiene un solo rol. La coordinación queda vinculada a una facultad y el docente a un programa; el coordinador también se evalúa como docente."
      >
        <Boton icono={UserPlus} onClick={() => navegar('/usuarios/nuevo', { state: { conservarFoco: true } })}>
          Nuevo usuario
        </Boton>
      </Encabezado>

      <div className="pagina">
        <MensajeNavegacion />
        <div aria-live="polite">{resultado && <Aviso tipo={resultado.tipo} titulo={resultado.texto} />}</div>

        {resumen.length > 0 && (
          <section className="usuarios__resumen" aria-labelledby="titulo-resumen-usuarios">
            <h2 id="titulo-resumen-usuarios" className="usuarios__resumen-titulo">
              <Building2 size={17} aria-hidden="true" /> Dónde están los usuarios
            </h2>
            <ul className="usuarios__facultades">
              {resumen.map((f) => (
                <li key={f.nombre}>
                  <button
                    type="button"
                    className="usuarios__facultad"
                    aria-pressed={facultad === f.nombre}
                    onClick={() => actualizarFiltros({ facultad: facultad === f.nombre ? TODOS : f.nombre })}
                  >
                    <span className="usuarios__facultad-nombre">{f.nombre}</span>
                    <strong className="usuarios__facultad-total">{f.total}</strong>
                    <span className="usuarios__facultad-roles">
                      {f.nombre === INSTITUCIONAL
                        ? 'Vicerrectoría, administración y visualización'
                        : ROLES_RESUMEN.filter((r) => f.roles[r])
                            .map((r) => `${f.roles[r]} ${NOMBRE_ROL[r][f.roles[r] === 1 ? 0 : 1]}`)
                            .join(' · ')}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          </section>
        )}

        <div className="usuarios__filtros">
          <label className="usuarios__buscador">
            <Search size={16} aria-hidden="true" />
            <span className="solo-lectores">Buscar usuario</span>
            <input
              type="search"
              value={busqueda}
              onChange={(e) => actualizarFiltros({ busqueda: e.target.value })}
              placeholder="Buscar por nombre, correo, programa o facultad"
            />
          </label>
          <label className="usuarios__filtro">
            <span className="usuarios__filtro-etiqueta">Rol</span>
            <select
              className="campo__control usuarios__selector"
              value={rol}
              onChange={(e) => actualizarFiltros({ rol: e.target.value })}
            >
              <option value={TODOS}>Todos los roles</option>
              {Object.values(ROLES).map((r) => (
                <option key={r} value={r}>
                  {ETIQUETA_ROL[r]}
                </option>
              ))}
            </select>
          </label>
          <label className="usuarios__filtro">
            <span className="usuarios__filtro-etiqueta">Facultad</span>
            <select
              className="campo__control usuarios__selector"
              value={facultad}
              onChange={(e) => actualizarFiltros({ facultad: e.target.value })}
            >
              <option value={TODOS}>Todas las facultades</option>
              {resumen.map((f) => (
                <option key={f.nombre} value={f.nombre}>
                  {f.nombre}
                </option>
              ))}
            </select>
          </label>
          <label className="usuarios__filtro">
            <span className="usuarios__filtro-etiqueta">Agrupar por</span>
            <select
              className="campo__control usuarios__selector"
              value={agrupar}
              onChange={(e) => actualizarFiltros({ agrupar: e.target.value })}
            >
              <option value={TODOS}>Sin agrupar</option>
              {Object.entries(AGRUPACIONES).map(([clave, { etiqueta }]) => (
                <option key={clave} value={clave}>
                  {etiqueta}
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
            <table className="tabla tabla--apilable usuarios__tabla">
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
              {grupos ? (
                grupos.map(([nombre, miembros]) => (
                  <tbody key={nombre} className="usuarios__grupo">
                    <tr>
                      <th scope="colgroup" colSpan={5} className="usuarios__grupo-titulo">
                        {nombre} <span className="usuarios__grupo-conteo">{miembros.length}</span>
                      </th>
                    </tr>
                    {miembros.map((u) => (
                      <FilaUsuario key={u.idUsuario} u={u} {...propsFila} />
                    ))}
                  </tbody>
                ))
              ) : (
                <tbody>
                  {paginacion.visibles.map((u) => (
                    <FilaUsuario key={u.idUsuario} u={u} {...propsFila} />
                  ))}
                </tbody>
              )}
            </table>
          </div>
        )}
        {!usuarios.cargando && !usuarios.error && !grupos && <Paginacion {...paginacion} etiqueta="usuarios" />}
      </div>

      {viendo && <ModalUsuario usuario={viendo} vinculo={vinculo(viendo)} onCerrar={() => setViendo(null)} />}

      {/* Modal de crear o editar (rutas hijas); al guardar recarga la lista */}
      <Outlet context={{ alGuardar: usuarios.recargar }} />
    </>
  )
}

/** Una fila de la tabla de usuarios. */
function FilaUsuario({ u, sesion, cambiando, onVer, onAlternar }) {
  const { principal, secundario } = vinculo(u)
  const esPropio = u.idUsuario === sesion.idUsuario
  return (
    <tr className={u.esActivo ? undefined : 'usuarios__fila--inactiva'}>
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
      <td data-etiqueta="Rol">
        <span className="pildora pildora--info">{ETIQUETA_ROL[u.rol]}</span>
        {u.rol === ROLES.COORDINADOR && (
          <span className="usuarios__doble-rol" title="Se evalúa como docente ante la vicerrectoría">
            También docente
          </span>
        )}
      </td>
      <td data-etiqueta="Vínculo">
        {principal ?? '—'}
        {secundario && <span className="tabla__tenue">{secundario}</span>}
      </td>
      <td data-etiqueta="Estado">
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
          <BotonAccion tipo="ver" nombre={u.nombreCompleto} onClick={() => onVer(u)} />
          <BotonAccion
            tipo="editar"
            nombre={u.nombreCompleto}
            a={`/usuarios/${u.idUsuario}`}
            state={{ conservarFoco: true }}
          />
          {!esPropio && (
            <button
              type="button"
              className={`usuarios__accion${u.esActivo ? ' usuarios__accion--peligro' : ''}`}
              onClick={() => onAlternar(u)}
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
}

/** Ficha de solo lectura de un usuario: rol, vínculo, estado y, si es docente, su expediente. */
function ModalUsuario({ usuario, vinculo, onCerrar }) {
  return (
    <Modal titulo={usuario.nombreCompleto} descripcion={usuario.correo} icono={UserRound} ancho="pequeno" onCerrar={onCerrar}>
      <dl className="usuarios__ficha">
        <div>
          <dt>Rol</dt>
          <dd>{ETIQUETA_ROL[usuario.rol]}</dd>
        </div>
        <div>
          <dt>Vínculo</dt>
          <dd>
            {vinculo.principal ?? 'Sin vínculo'}
            {vinculo.secundario && <span>{vinculo.secundario}</span>}
          </dd>
        </div>
        <div>
          <dt>Estado</dt>
          <dd>
            {usuario.esActivo ? 'Activo' : 'Inactivo'}
            {usuario.esActivo && usuario.debeCambiarContrasena && <span>Debe cambiar la contraseña temporal</span>}
          </dd>
        </div>
      </dl>
      <div className="acciones-fila">
        {usuario.rol === ROLES.DOCENTE && (
          <BotonAccion tipo="ver" texto="Ver expediente" nombre={usuario.nombreCompleto} a={`/docentes/${usuario.idUsuario}`} />
        )}
        <BotonAccion tipo="editar" nombre={usuario.nombreCompleto} a={`/usuarios/${usuario.idUsuario}`} />
      </div>
    </Modal>
  )
}
