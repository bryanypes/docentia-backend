import { Check, Copy, Eye, EyeOff, RefreshCw, Save, UserCog, UserPlus } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useOutletContext, useParams } from 'react-router-dom'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Modal } from '../../../shared/components/ui/Modal/Modal.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ResumenErrores } from '../../../shared/components/ui/ResumenErrores/ResumenErrores.jsx'
import { ETIQUETA_ROL, ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { generarContrasenaTemporal } from '../../../shared/utils/contrasena.js'
import { validarUsuario } from '../../../shared/utils/validators.js'
import { useSesion } from '../../auth/sesionContext.js'
import { listarFacultades } from '../../facultades/services/facultadesService.js'
import { listarProgramas } from '../../programas/services/programasService.js'
import { actualizarUsuario, crearUsuario, obtenerUsuario } from '../services/usuariosService.js'
import './FormularioUsuario.css'

/** En el orden en que más se usan: casi todas las cuentas nuevas son de docentes. */
const ROLES_ORDENADOS = [ROLES.DOCENTE, ROLES.COORDINADOR, ROLES.VICERRECTOR]

const DESCRIPCION_ROL = {
  VICERRECTOR: 'Alcance institucional: administra periodos, usuarios y ve todas las facultades.',
  COORDINADOR: 'Ve y califica a los docentes de su facultad y carga el CSV de estudiantes.',
  DOCENTE: 'Consulta su propia evaluación, sin notas individuales por fuente.',
}

/** Orden de los campos en pantalla, para el resumen de errores; `id` es el control que recibe el foco. */
const CAMPOS = [
  { campo: 'nombreCompleto', id: 'nombreCompleto' },
  { campo: 'correo', id: 'correo' },
  { campo: 'contrasena', id: 'contrasena' },
  { campo: 'rol', id: `rol-${ROLES_ORDENADOS[0]}` },
  { campo: 'idFacultad', id: 'idFacultad' },
  { campo: 'idPrograma', id: 'idPrograma' },
]

function formularioDesde(usuario) {
  return {
    nombreCompleto: usuario?.nombreCompleto ?? '',
    correo: usuario?.correo ?? '',
    rol: usuario?.rol ?? '',
    idFacultad: usuario?.idFacultad ? String(usuario.idFacultad) : '',
    idPrograma: usuario?.idPrograma ? String(usuario.idPrograma) : '',
    contrasena: '',
  }
}

/** Cuerpo de UsuarioRequest: el vínculo depende del rol y lo que no aplica va en null. */
function cuerpoDesde(formulario) {
  const { rol } = formulario
  return {
    nombreCompleto: formulario.nombreCompleto.trim(),
    correo: formulario.correo.trim(),
    rol,
    // La facultad del docente es la de su programa: aquí solo sirve para filtrar la lista.
    idFacultad: rol === ROLES.COORDINADOR ? Number(formulario.idFacultad) : null,
    idPrograma: rol === ROLES.DOCENTE ? Number(formulario.idPrograma) : null,
    contrasena: formulario.contrasena || null,
  }
}

const mismoFormulario = (a, b) => Object.keys(a).every((campo) => a[campo] === b[campo])

/** Programas agrupados por facultad, para el <optgroup> cuando no se filtra por facultad. */
function agruparPorFacultad(programas) {
  const grupos = new Map()
  for (const programa of programas) {
    if (!grupos.has(programa.nombreFacultad)) grupos.set(programa.nombreFacultad, [])
    grupos.get(programa.nombreFacultad).push(programa)
  }
  return [...grupos.entries()]
}

// Cerrar el modal vuelve a la lista sin mover el foco al contenido (lo devuelve el propio diálogo).
const VOLVER_A_LISTA = ['/usuarios', { state: { conservarFoco: true } }]

/**
 * Crear o editar un usuario en una ventana modal sobre la lista. Las rutas /usuarios/nuevo y
 * /usuarios/:id se renderizan dentro de Usuarios, así un enlace directo también abre el modal.
 */
export default function FormularioUsuario() {
  const { idUsuario } = useParams()
  const esNuevo = !idUsuario
  const navegar = useNavigate()
  // El formulario registra aquí su forma de salir (pregunta si hay cambios sin guardar).
  const intentarCerrar = useRef(null)
  const registrarSalida = useCallback((salir) => {
    intentarCerrar.current = salir
  }, [])

  const cargar = useCallback(() => (esNuevo ? Promise.resolve(null) : obtenerUsuario(idUsuario)), [esNuevo, idUsuario])
  const usuario = useFetch(cargar, [cargar])

  const cerrar = () => (intentarCerrar.current ? intentarCerrar.current() : navegar(...VOLVER_A_LISTA))

  return (
    <Modal
      titulo={esNuevo ? 'Nuevo usuario' : 'Editar usuario'}
      descripcion="El rol define qué ve y qué puede hacer la persona. La contraseña que asignes es temporal: la cambiará al ingresar."
      icono={esNuevo ? UserPlus : UserCog}
      onCerrar={cerrar}
    >
      {usuario.cargando ? (
        <Cargando texto="Cargando usuario" bloques={2} />
      ) : usuario.error ? (
        <EstadoVista
          tipo={usuario.error.status === 404 ? 'vacio' : 'error'}
          titulo={usuario.error.status === 404 ? 'Usuario no encontrado' : 'No fue posible cargar el usuario'}
          mensaje={usuario.error.status === 404 ? 'El usuario no existe.' : usuario.error.message}
        >
          {usuario.error.status !== 404 && (
            <Boton icono={RefreshCw} onClick={usuario.recargar}>
              Intentar de nuevo
            </Boton>
          )}
        </EstadoVista>
      ) : (
        <Formulario key={idUsuario ?? 'nuevo'} guardado={usuario.datos} registrarSalida={registrarSalida} />
      )}
    </Modal>
  )
}

function Formulario({ guardado, registrarSalida }) {
  const navegar = useNavigate()
  const { alGuardar } = useOutletContext() ?? {}
  const { usuario: sesion } = useSesion()
  const esNuevo = !guardado
  // La vicerrectoría no puede quitarse su propio rol: perdería el acceso a esta misma pantalla.
  const esPropio = !esNuevo && guardado.idUsuario === sesion.idUsuario

  const [inicial] = useState(() => formularioDesde(guardado))
  const [formulario, setFormulario] = useState(inicial)
  const [intentoEnviar, setIntentoEnviar] = useState(false)
  const [intentosFallidos, setIntentosFallidos] = useState(0)
  const [enviando, setEnviando] = useState(false)
  const [errorEnvio, setErrorEnvio] = useState(null)
  const [verContrasena, setVerContrasena] = useState(false)
  const [copiada, setCopiada] = useState(false)
  const resumen = useRef(null)

  const facultades = useFetch(listarFacultades)
  const cargarProgramas = useCallback(() => listarProgramas(formulario.idFacultad || null), [formulario.idFacultad])
  const programas = useFetch(cargarProgramas, [cargarProgramas])

  const erroresLocales = validarUsuario(formulario, { contrasenaObligatoria: esNuevo })
  const erroresServidor = errorEnvio?.errores ?? {}
  // El correo duplicado llega como 409 sin campo: se muestra junto al correo.
  const errorCorreo409 = errorEnvio?.status === 409 ? errorEnvio.message : null
  const errorDe = (campo) =>
    erroresServidor[campo] ??
    (campo === 'correo' ? errorCorreo409 : null) ??
    (intentoEnviar ? erroresLocales[campo] : null)

  const { rol } = formulario
  // El error de facultad solo aplica a coordinación; el de programa, solo a docentes.
  const aplica = (campo) =>
    (campo !== 'idFacultad' || rol === ROLES.COORDINADOR) && (campo !== 'idPrograma' || rol === ROLES.DOCENTE)
  const listaErrores = CAMPOS.filter(({ campo }) => aplica(campo) && errorDe(campo)).map(({ campo, id }) => ({
    id,
    mensaje: errorDe(campo),
  }))

  const hayCambios = !mismoFormulario(formulario, inicial)
  const cambiaVinculo =
    !esNuevo &&
    (rol !== guardado.rol ||
      (rol === ROLES.COORDINADOR && formulario.idFacultad !== String(guardado.idFacultad ?? '')) ||
      (rol === ROLES.DOCENTE && formulario.idPrograma !== String(guardado.idPrograma ?? '')))

  // Cerrar o recargar la pestaña con cambios sin guardar pide confirmación al navegador.
  useEffect(() => {
    if (!hayCambios || enviando) return
    const avisar = (evento) => evento.preventDefault()
    window.addEventListener('beforeunload', avisar)
    return () => window.removeEventListener('beforeunload', avisar)
  }, [hayCambios, enviando])

  // Tras un envío con errores, el foco va al resumen para que se lea qué corregir.
  useEffect(() => {
    if (intentosFallidos > 0) resumen.current?.focus()
  }, [intentosFallidos])

  function cambiar(campo, valor) {
    setFormulario((f) => {
      const siguiente = { ...f, [campo]: valor }
      // Al cambiar de facultad el programa elegido puede dejar de pertenecerle.
      if (campo === 'idFacultad') siguiente.idPrograma = ''
      return siguiente
    })
    if (campo === 'contrasena') setCopiada(false)
    setErrorEnvio(null)
  }

  function generarContrasena() {
    cambiar('contrasena', generarContrasenaTemporal())
    // Una contraseña generada hay que leerla para comunicarla.
    setVerContrasena(true)
  }

  async function copiarContrasena() {
    try {
      await navigator.clipboard.writeText(formulario.contrasena)
      setCopiada(true)
    } catch {
      setCopiada(false)
    }
  }

  function salir() {
    if (hayCambios && !window.confirm('Tienes cambios sin guardar. ¿Salir sin guardarlos?')) return
    navegar(...VOLVER_A_LISTA)
  }
  useEffect(() => {
    registrarSalida(salir)
  })

  async function guardar(evento) {
    evento.preventDefault()
    setIntentoEnviar(true)
    if (Object.keys(erroresLocales).some(aplica)) {
      setIntentosFallidos((n) => n + 1)
      return
    }

    setEnviando(true)
    setErrorEnvio(null)
    try {
      const cuerpo = cuerpoDesde(formulario)
      const resultado = esNuevo
        ? await crearUsuario(cuerpo)
        : await actualizarUsuario(guardado.idUsuario, cuerpo)
      alGuardar?.()
      navegar('/usuarios', {
        state: {
          conservarFoco: true,
          mensaje: `Se ${esNuevo ? 'creó' : 'actualizó'} la cuenta de ${resultado.nombreCompleto} con el rol ${ETIQUETA_ROL[resultado.rol]}.`,
        },
      })
    } catch (error) {
      setErrorEnvio(error)
      setEnviando(false)
      if (Object.keys(error.errores ?? {}).length > 0 || error.status === 409) setIntentosFallidos((n) => n + 1)
    }
  }

  // Los errores con campo (400 de validación, 409 de correo) van en el resumen; el resto, en un aviso.
  const mensajeGeneral =
    errorEnvio && errorEnvio.status !== 409 && Object.keys(erroresServidor).length === 0 ? errorEnvio.message : null

  const describir = (id, ...extra) => [...extra, errorDe(id) && `${id}-error`].filter(Boolean).join(' ') || undefined
  const listaProgramas = programas.datos ?? []

  return (
    <>
      <form className="formulario-usuario" onSubmit={guardar} noValidate>
        <ResumenErrores ref={resumen} errores={listaErrores} />

        <Panel titulo="Datos de la persona">
          <div className="formulario-usuario__campos">
            <Campo id="nombreCompleto" etiqueta="Nombre completo" error={errorDe('nombreCompleto')}>
              <input
                id="nombreCompleto"
                className="campo__control"
                value={formulario.nombreCompleto}
                maxLength={150}
                autoComplete="off"
                onChange={(e) => cambiar('nombreCompleto', e.target.value)}
                aria-invalid={Boolean(errorDe('nombreCompleto'))}
                aria-describedby={describir('nombreCompleto')}
              />
            </Campo>

            <Campo
              id="correo"
              etiqueta="Correo institucional"
              error={errorDe('correo')}
              ayuda="Es el usuario para iniciar sesión. Se guarda en minúsculas."
            >
              <input
                id="correo"
                type="email"
                inputMode="email"
                autoCapitalize="none"
                spellCheck={false}
                className="campo__control"
                value={formulario.correo}
                maxLength={150}
                autoComplete="off"
                placeholder="nombre@uniautonoma.edu.co"
                onChange={(e) => cambiar('correo', e.target.value)}
                aria-invalid={Boolean(errorDe('correo'))}
                aria-describedby={describir('correo', 'correo-ayuda')}
              />
            </Campo>
          </div>

          <div className="formulario-usuario__contrasena">
            <Campo
              id="contrasena"
              etiqueta={esNuevo ? 'Contraseña temporal' : 'Nueva contraseña (opcional)'}
              error={errorDe('contrasena')}
              ayuda={
                esNuevo
                  ? 'Entre 8 y 72 caracteres. Genérala y cópiala para entregársela a la persona.'
                  : 'Déjala vacía para conservar la actual. Úsala para restablecer el acceso si la olvidó.'
              }
            >
              <div className="formulario-usuario__control-contrasena">
                <input
                  id="contrasena"
                  type={verContrasena ? 'text' : 'password'}
                  className="campo__control"
                  value={formulario.contrasena}
                  maxLength={72}
                  autoComplete="new-password"
                  spellCheck={false}
                  onChange={(e) => cambiar('contrasena', e.target.value)}
                  aria-invalid={Boolean(errorDe('contrasena'))}
                  aria-describedby={describir('contrasena', 'contrasena-ayuda')}
                />
                <button
                  type="button"
                  className="formulario-usuario__icono-boton"
                  onClick={() => setVerContrasena((v) => !v)}
                  aria-pressed={verContrasena}
                  aria-label="Mostrar contraseña"
                  aria-controls="contrasena"
                >
                  {verContrasena ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
                </button>
              </div>
            </Campo>
            <div className="formulario-usuario__acciones-contrasena">
              <Boton variante="contorno" icono={RefreshCw} onClick={generarContrasena}>
                Generar
              </Boton>
              <Boton variante="secundario" onClick={copiarContrasena} disabled={!formulario.contrasena}>
                {copiada ? <Check size={16} aria-hidden="true" /> : <Copy size={16} aria-hidden="true" />}
                {copiada ? 'Copiada' : 'Copiar'}
              </Boton>
              <span className="solo-lectores" role="status">
                {copiada ? 'Contraseña copiada al portapapeles' : ''}
              </span>
            </div>
          </div>
        </Panel>

        <Panel titulo="Rol y vínculo" descripcion="Un usuario tiene un solo rol. Al cambiarlo, el vínculo anterior se reemplaza.">
          {esPropio && (
            <Aviso tipo="info" titulo="Es tu propia cuenta">
              No puedes cambiar tu rol desde aquí: perderías el acceso a la gestión de usuarios. Pídeselo a otra
              persona con rol de Vicerrectoría.
            </Aviso>
          )}
          <fieldset
            className="formulario-usuario__roles"
            aria-describedby={errorDe('rol') ? 'rol-error' : undefined}
            disabled={esPropio}
          >
            <legend className="rotulo">Rol</legend>
            {ROLES_ORDENADOS.map((r) => (
              <label
                key={r}
                htmlFor={`rol-${r}`}
                className={`formulario-usuario__rol${rol === r ? ' formulario-usuario__rol--activo' : ''}`}
              >
                <input
                  id={`rol-${r}`}
                  type="radio"
                  name="rol"
                  value={r}
                  checked={rol === r}
                  onChange={() => cambiar('rol', r)}
                  aria-invalid={Boolean(errorDe('rol'))}
                />
                <span>
                  <strong>{ETIQUETA_ROL[r]}</strong>
                  <span>{DESCRIPCION_ROL[r]}</span>
                </span>
              </label>
            ))}
            {errorDe('rol') && (
              <span id="rol-error" className="campo__error">
                {errorDe('rol')}
              </span>
            )}
          </fieldset>

          {(rol === ROLES.COORDINADOR || rol === ROLES.DOCENTE) && (
            <div className="formulario-usuario__campos formulario-usuario__vinculo">
              <Campo
                id="idFacultad"
                etiqueta={rol === ROLES.DOCENTE ? 'Filtrar programas por facultad (opcional)' : 'Facultad que coordina'}
                error={rol === ROLES.COORDINADOR ? errorDe('idFacultad') : null}
              >
                <select
                  id="idFacultad"
                  className="campo__control"
                  value={formulario.idFacultad}
                  onChange={(e) => cambiar('idFacultad', e.target.value)}
                  disabled={facultades.cargando}
                  aria-invalid={Boolean(rol === ROLES.COORDINADOR && errorDe('idFacultad'))}
                  aria-describedby={rol === ROLES.COORDINADOR ? describir('idFacultad') : undefined}
                >
                  <option value="">
                    {facultades.cargando
                      ? 'Cargando facultades…'
                      : rol === ROLES.DOCENTE
                        ? 'Todas las facultades'
                        : 'Elige una facultad'}
                  </option>
                  {(facultades.datos ?? []).map((f) => (
                    <option key={f.idFacultad} value={f.idFacultad}>
                      {f.nombre}
                    </option>
                  ))}
                </select>
              </Campo>

              {rol === ROLES.DOCENTE && (
                <Campo id="idPrograma" etiqueta="Programa del docente" error={errorDe('idPrograma')}>
                  <select
                    id="idPrograma"
                    className="campo__control"
                    value={formulario.idPrograma}
                    onChange={(e) => cambiar('idPrograma', e.target.value)}
                    disabled={programas.cargando}
                    aria-invalid={Boolean(errorDe('idPrograma'))}
                    aria-describedby={describir('idPrograma')}
                  >
                    <option value="">{programas.cargando ? 'Cargando programas…' : 'Elige un programa'}</option>
                    {formulario.idFacultad
                      ? listaProgramas.map((p) => (
                          <option key={p.idPrograma} value={p.idPrograma}>
                            {p.nombre}
                          </option>
                        ))
                      : agruparPorFacultad(listaProgramas).map(([facultad, lista]) => (
                          <optgroup key={facultad} label={facultad}>
                            {lista.map((p) => (
                              <option key={p.idPrograma} value={p.idPrograma}>
                                {p.nombre}
                              </option>
                            ))}
                          </optgroup>
                        ))}
                  </select>
                </Campo>
              )}
            </div>
          )}

          {(facultades.error || programas.error) && (
            <Aviso tipo="error" titulo="No fue posible cargar facultades o programas">
              {(facultades.error ?? programas.error).message}
            </Aviso>
          )}
        </Panel>

        <Panel>
          {cambiaVinculo && (
            <Aviso tipo="alerta" titulo="La sesión abierta conserva el alcance anterior">
              Si la persona tiene una sesión iniciada, verá su rol y vínculo anteriores hasta que vuelva a
              iniciar sesión o pasen 8 horas.
            </Aviso>
          )}
          {mensajeGeneral && <Aviso tipo="error">{mensajeGeneral}</Aviso>}
          <div className="formulario-usuario__botones">
            <Boton type="submit" icono={Save} disabled={enviando || (!esNuevo && !hayCambios)} aria-busy={enviando}>
              {enviando ? 'Guardando…' : esNuevo ? 'Crear usuario' : 'Guardar cambios'}
            </Boton>
            <Boton variante="secundario" onClick={salir} disabled={enviando}>
              Cancelar
            </Boton>
            {!esNuevo && !hayCambios && <span className="formulario-usuario__sin-cambios">Sin cambios por guardar</span>}
          </div>
        </Panel>
      </form>
    </>
  )
}

function Campo({ id, etiqueta, ayuda, error, children }) {
  return (
    <div className="campo">
      <label htmlFor={id} className="formulario-usuario__etiqueta">
        {etiqueta}
      </label>
      {children}
      {ayuda && (
        <span id={`${id}-ayuda`} className="campo__ayuda">
          {ayuda}
        </span>
      )}
      {error && (
        <span id={`${id}-error`} className="campo__error">
          {error}
        </span>
      )}
    </div>
  )
}
