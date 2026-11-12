import { BookOpen, Building2, Plus, RefreshCw, Search } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { AvisoFlotante } from '../../../shared/components/ui/Avisos/AvisoFlotante.jsx'
import { useAvisos } from '../../../shared/components/ui/Avisos/avisosContext.js'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { BotonAccion } from '../../../shared/components/ui/BotonAccion/BotonAccion.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Modal } from '../../../shared/components/ui/Modal/Modal.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { normalizarTexto } from '../../../shared/utils/formatters.js'
import {
  actualizarPrograma,
  crearPrograma,
  eliminarPrograma,
  listarProgramas,
} from '../../programas/services/programasService.js'
import { ModalAlcance } from '../components/ModalAlcance.jsx'
import {
  actualizarFacultad,
  crearFacultad,
  eliminarFacultad,
  listarFacultades,
} from '../services/facultadesService.js'
import './Facultades.css'

const ORDENES = {
  AZ: { etiqueta: 'Nombre (A-Z)', comparar: (a, b) => a.nombre.localeCompare(b.nombre, 'es') },
  ZA: { etiqueta: 'Nombre (Z-A)', comparar: (a, b) => b.nombre.localeCompare(a.nombre, 'es') },
  PROGRAMAS: {
    etiqueta: 'Más programas',
    comparar: (a, b) => b.programas.length - a.programas.length || a.nombre.localeCompare(b.nombre, 'es'),
  },
}

const plural = (n, uno, varios) => `${n} ${n === 1 ? uno : varios}`

/**
 * Facultades y programas en una sola página: cada facultad es una tarjeta con sus programas. Crear,
 * editar, ver y eliminar se hace en modales; el buscador encuentra facultades o programas.
 */
export default function Facultades() {
  const { confirmar } = useAvisos()
  const facultades = useFetch(listarFacultades)
  const programas = useFetch(() => listarProgramas(null), [])
  const [busqueda, setBusqueda] = useState('')
  const [orden, setOrden] = useState('AZ')
  // { tipo: 'facultad' | 'programa', registro?, idFacultad? }
  const [edicion, setEdicion] = useState(null)
  const [viendo, setViendo] = useState(null)
  const [error, setError] = useState(null)
  const [resultado, setResultado] = useState(null)

  const tarjetas = useMemo(() => {
    const porFacultad = new Map()
    for (const p of programas.datos ?? []) {
      const lista = porFacultad.get(p.idFacultad) ?? []
      lista.push(p)
      porFacultad.set(p.idFacultad, lista)
    }
    const termino = normalizarTexto(busqueda.trim())
    return (facultades.datos ?? [])
      .map((f) => {
        const todos = (porFacultad.get(f.idFacultad) ?? []).sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
        if (!termino) return { ...f, programas: todos, coincide: false }
        const coincideFacultad = normalizarTexto(f.nombre).includes(termino)
        const coincidentes = todos.filter((p) => normalizarTexto(p.nombre).includes(termino))
        if (!coincideFacultad && coincidentes.length === 0) return null
        return { ...f, programas: coincideFacultad ? todos : coincidentes, coincide: true }
      })
      .filter(Boolean)
      .sort(ORDENES[orden].comparar)
  }, [facultades.datos, programas.datos, busqueda, orden])

  function recargar() {
    facultades.recargar()
    programas.recargar()
  }

  async function guardar(datos) {
    setError(null)
    setResultado(null)
    const { tipo, registro } = edicion
    if (tipo === 'facultad') {
      await (registro ? actualizarFacultad(registro.idFacultad, datos) : crearFacultad(datos))
      setResultado(`Facultad ${datos.nombre} ${registro ? 'actualizada' : 'creada'}.`)
    } else {
      await (registro ? actualizarPrograma(registro.idPrograma, datos) : crearPrograma(datos))
      setResultado(`Programa ${datos.nombre} ${registro ? 'actualizado' : 'creado'}.`)
    }
    setEdicion(null)
    recargar()
  }

  async function eliminar(tipo, registro) {
    const esFacultad = tipo === 'facultad'
    const seguro = await confirmar({
      tipo: 'peligro',
      titulo: esFacultad ? '¿Eliminar la facultad?' : '¿Eliminar el programa?',
      mensaje: esFacultad
        ? `«${registro.nombre}» se elimina si no tiene programas ni usuarios.`
        : `«${registro.nombre}» se elimina si no tiene docentes.`,
      textoConfirmar: 'Eliminar',
    })
    if (!seguro) return
    setError(null)
    setResultado(null)
    try {
      await (esFacultad ? eliminarFacultad(registro.idFacultad) : eliminarPrograma(registro.idPrograma))
      setResultado(`${esFacultad ? 'Facultad' : 'Programa'} ${registro.nombre} ${esFacultad ? 'eliminada' : 'eliminado'}.`)
      recargar()
    } catch (errorApi) {
      setError(errorApi)
    }
  }

  const cargando = (facultades.cargando && !facultades.datos) || (programas.cargando && !programas.datos)
  const errorCarga = facultades.error ?? programas.error
  const totalProgramas = (programas.datos ?? []).length

  return (
    <>
      <Encabezado
        miga="Vicerrectoría · Facultades y programas"
        titulo="Facultades y programas"
        subtitulo="Cada facultad con sus programas académicos. Ve, edita o agrega desde su tarjeta."
      >
        <Boton icono={Plus} onClick={() => setEdicion({ tipo: 'facultad' })}>
          Nueva facultad
        </Boton>
      </Encabezado>
      <div className="pagina">
        <AvisoFlotante mensaje={resultado} />
        <ErrorApi error={error} flotante />

        <div className="facultades__barra" role="search" aria-label="Buscar facultades y programas">
          <label className="facultades__buscador">
            <Search size={16} aria-hidden="true" />
            <span className="solo-lectores">Buscar facultad o programa</span>
            <input
              type="search"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              placeholder="Buscar facultad o programa"
            />
          </label>
          <label className="facultades__orden">
            <span>Ordenar</span>
            <select className="campo__control" value={orden} onChange={(e) => setOrden(e.target.value)}>
              {Object.entries(ORDENES).map(([clave, { etiqueta }]) => (
                <option key={clave} value={clave}>
                  {etiqueta}
                </option>
              ))}
            </select>
          </label>
          <span className="facultades__conteo" aria-live="polite">
            {plural(tarjetas.length, 'facultad', 'facultades')} · {plural(totalProgramas, 'programa', 'programas')}
          </span>
        </div>

        {cargando && <Cargando texto="Cargando facultades y programas" bloques={2} />}
        {errorCarga && (
          <EstadoVista tipo="error" titulo="No fue posible cargar las facultades" mensaje={errorCarga.message}>
            <Boton icono={RefreshCw} onClick={recargar}>
              Intentar de nuevo
            </Boton>
          </EstadoVista>
        )}
        {!cargando && !errorCarga && (facultades.datos ?? []).length === 0 && (
          <EstadoVista titulo="Aún no hay facultades" mensaje="Crea la primera con «Nueva facultad»." />
        )}
        {!cargando && !errorCarga && (facultades.datos ?? []).length > 0 && tarjetas.length === 0 && (
          <EstadoVista titulo="Ninguna facultad o programa coincide" mensaje="Prueba con otra palabra." />
        )}

        <div className="facultades__rejilla">
          {tarjetas.map((f) => (
            <section key={f.idFacultad} className="facultad" aria-labelledby={`facultad-${f.idFacultad}`}>
              <header className="facultad__cabecera">
                <span className="facultad__icono" aria-hidden="true">
                  <Building2 size={20} />
                </span>
                <div className="facultad__titulo">
                  <h2 id={`facultad-${f.idFacultad}`}>{f.nombre}</h2>
                  <span>{plural(f.programas.length, 'programa', 'programas')}{f.coincide ? ' que coinciden' : ''}</span>
                </div>
                <div className="acciones-fila facultad__acciones">
                  <BotonAccion tipo="ver" nombre={f.nombre} onClick={() => setViendo({ facultad: f })} />
                  <BotonAccion tipo="editar" nombre={f.nombre} onClick={() => setEdicion({ tipo: 'facultad', registro: f })} />
                  <BotonAccion tipo="eliminar" nombre={f.nombre} onClick={() => eliminar('facultad', f)} />
                </div>
              </header>

              {f.programas.length === 0 ? (
                <p className="facultad__vacia">Esta facultad todavía no tiene programas.</p>
              ) : (
                <ul className="facultad__programas">
                  {f.programas.map((p) => (
                    <li key={p.idPrograma}>
                      <BookOpen size={16} aria-hidden="true" />
                      <span className="facultad__programa">{p.nombre}</span>
                      <div className="acciones-fila">
                        <BotonAccion tipo="ver" nombre={p.nombre} onClick={() => setViendo({ programa: p })} />
                        <BotonAccion tipo="editar" nombre={p.nombre} onClick={() => setEdicion({ tipo: 'programa', registro: p })} />
                        <BotonAccion tipo="eliminar" nombre={p.nombre} onClick={() => eliminar('programa', p)} />
                      </div>
                    </li>
                  ))}
                </ul>
              )}

              <button
                type="button"
                className="facultad__agregar"
                onClick={() => setEdicion({ tipo: 'programa', idFacultad: f.idFacultad })}
              >
                <Plus size={16} aria-hidden="true" />
                Agregar programa a {f.nombre}
              </button>
            </section>
          ))}
        </div>

        {viendo && <ModalAlcance {...viendo} onCerrar={() => setViendo(null)} />}
        {edicion && (
          <ModalEdicion
            edicion={edicion}
            facultades={facultades.datos ?? []}
            onGuardar={guardar}
            onCerrar={() => setEdicion(null)}
          />
        )}
      </div>
    </>
  )
}

/** Crear o editar una facultad (nombre) o un programa (nombre y facultad). */
function ModalEdicion({ edicion, facultades, onGuardar, onCerrar }) {
  const { tipo, registro, idFacultad } = edicion
  const esFacultad = tipo === 'facultad'
  const [nombre, setNombre] = useState(registro?.nombre ?? '')
  const [facultad, setFacultad] = useState(String(registro?.idFacultad ?? idFacultad ?? ''))
  const [error, setError] = useState(null)
  const [guardando, setGuardando] = useState(false)

  async function enviar(evento) {
    evento.preventDefault()
    setGuardando(true)
    setError(null)
    try {
      await onGuardar(esFacultad ? { nombre: nombre.trim() } : { nombre: nombre.trim(), idFacultad: Number(facultad) })
    } catch (errorApi) {
      setError(errorApi)
      setGuardando(false)
    }
  }

  const titulo = esFacultad
    ? registro ? 'Editar facultad' : 'Nueva facultad'
    : registro ? 'Editar programa' : 'Nuevo programa'
  return (
    <Modal titulo={titulo} icono={esFacultad ? Building2 : BookOpen} ancho="pequeno" onCerrar={onCerrar}>
      <form className="facultades__formulario" onSubmit={enviar}>
        <ErrorApi error={error} nombresCampos={{ nombre: 'Nombre', idFacultad: 'Facultad' }} />
        <Campo id="nombreRegistro" etiqueta="Nombre">
          <input
            id="nombreRegistro"
            className="campo__control"
            value={nombre}
            maxLength={120}
            onChange={(e) => setNombre(e.target.value)}
            required
          />
        </Campo>
        {!esFacultad && (
          <Campo id="facultadRegistro" etiqueta="Facultad">
            <select
              id="facultadRegistro"
              className="campo__control"
              value={facultad}
              onChange={(e) => setFacultad(e.target.value)}
              required
            >
              <option value="">Elige una facultad</option>
              {facultades.map((f) => (
                <option key={f.idFacultad} value={f.idFacultad}>
                  {f.nombre}
                </option>
              ))}
            </select>
          </Campo>
        )}
        <div className="acciones-fila facultades__botones">
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" disabled={guardando || !nombre.trim() || (!esFacultad && !facultad)}>
            {guardando ? 'Guardando…' : registro ? 'Guardar cambios' : esFacultad ? 'Crear facultad' : 'Crear programa'}
          </Boton>
        </div>
      </form>
    </Modal>
  )
}
