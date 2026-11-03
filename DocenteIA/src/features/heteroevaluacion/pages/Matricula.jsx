import { Download, FileSpreadsheet, RefreshCw, Search, ShieldCheck, Upload, UserCheck, Users, UserX } from 'lucide-react'
import { useCallback, useMemo, useRef, useState } from 'react'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { BotonAccion } from '../../../shared/components/ui/BotonAccion/BotonAccion.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETIQUETA_ROL } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { normalizarTexto } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { descargarPlantilla, importarMatricula, obtenerCobertura } from '../services/matriculaService.js'
import './Matricula.css'

function porcentaje(valor) {
  return `${Number(valor).toLocaleString('es-CO')} %`
}

/** Semáforo de la cobertura: el color acompaña al número, nunca lo reemplaza. */
function tonoCobertura(valor) {
  const n = Number(valor)
  if (n >= 80) return 'alta'
  if (n >= 50) return 'media'
  return 'baja'
}

function Importacion({ alImportar }) {
  const [archivo, setArchivo] = useState(null)
  const [arrastrando, setArrastrando] = useState(false)
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState(null)
  const [ocupado, setOcupado] = useState(false)
  const enCurso = useRef(false)

  async function importar() {
    if (enCurso.current || !archivo) return
    enCurso.current = true
    setOcupado(true)
    setError(null)
    setResultado(null)
    try {
      setResultado(await importarMatricula(archivo))
      alImportar()
    } catch (errorApi) {
      if (errorApi.status === 422 && errorApi.datos?.errores) setResultado(errorApi.datos)
      else setError(errorApi)
    } finally {
      enCurso.current = false
      setOcupado(false)
    }
  }

  async function plantilla() {
    setError(null)
    try {
      const blob = await descargarPlantilla()
      const url = URL.createObjectURL(blob)
      const enlace = document.createElement('a')
      enlace.href = url
      enlace.download = 'plantilla-matricula.csv'
      enlace.click()
      URL.revokeObjectURL(url)
    } catch (errorApi) {
      setError(errorApi)
    }
  }

  function soltar(evento) {
    evento.preventDefault()
    setArrastrando(false)
    const elegido = evento.dataTransfer.files?.[0]
    if (elegido) setArchivo(elegido)
  }

  return (
    <Panel
      titulo="Importar matrícula"
      descripcion="CSV del periodo activo. Si una fila tiene error, no se guarda nada."
      icono={Upload}
      tono="cielo"
    >
      <ol className="matricula__pasos">
        <li>
          <span className="matricula__paso">1</span>
          <div>
            <strong>Descarga la plantilla</strong>
            <span>Trae las columnas que espera el sistema.</span>
            <Boton variante="contorno" icono={Download} onClick={plantilla}>
              Descargar plantilla
            </Boton>
          </div>
        </li>
        <li>
          <span className="matricula__paso">2</span>
          <div>
            <strong>Elige el archivo</strong>
            <label
              htmlFor="archivoMatricula"
              className={`matricula__zona${arrastrando ? ' matricula__zona--activa' : ''}${archivo ? ' matricula__zona--lista' : ''}`}
              onDragOver={(e) => {
                e.preventDefault()
                setArrastrando(true)
              }}
              onDragLeave={() => setArrastrando(false)}
              onDrop={soltar}
            >
              <FileSpreadsheet size={26} aria-hidden="true" />
              <span className="matricula__zona-texto">
                {archivo ? archivo.name : 'Arrastra el CSV aquí o pulsa para buscarlo'}
              </span>
              <span className="matricula__zona-ayuda">Coma o punto y coma, UTF-8, máximo 5 MB.</span>
              <span className="solo-lectores">Archivo CSV</span>
              <input
                id="archivoMatricula"
                type="file"
                accept=".csv,text/csv"
                className="matricula__archivo"
                aria-label="Archivo CSV"
                onChange={(e) => setArchivo(e.target.files?.[0] ?? null)}
              />
            </label>
          </div>
        </li>
        <li>
          <span className="matricula__paso">3</span>
          <div>
            <strong>Importa</strong>
            <span>Se valida todo antes de guardar.</span>
            <Boton icono={Upload} disabled={!archivo || ocupado} onClick={importar}>
              {ocupado ? 'Importando…' : 'Importar'}
            </Boton>
          </div>
        </li>
      </ol>
      <ErrorApi error={error} />
      {resultado?.valido && (
        <Aviso tipo="exito" titulo="Matrícula importada">
          {resultado.filasProcesadas} filas · {resultado.estudiantesCreados} estudiantes nuevos ·{' '}
          {resultado.matriculasNuevas} matrículas nuevas · {resultado.matriculasRetiradas} retiradas
        </Aviso>
      )}
      {resultado && !resultado.valido && (
        <Aviso tipo="error" titulo="No se importó nada: corrige estas filas">
          <ul>
            {resultado.errores.map((e) => (
              <li key={`${e.fila}-${e.motivo}`}>
                Fila {e.fila}: {e.motivo}
              </li>
            ))}
          </ul>
        </Aviso>
      )}
    </Panel>
  )
}

/**
 * Cobertura como barras con semáforo: matriculados, respondieron y porcentaje. Solo cuenta; nunca
 * dice quién respondió. Con `conEnlace` cada docente lleva a su expediente.
 */
export function TablaCobertura({ filas, primeraColumna, conEnlace = false }) {
  return (
    <ul className="cobertura" aria-label={`Cobertura por ${primeraColumna.toLowerCase()}`}>
      {filas.map((f) => {
        const tono = tonoCobertura(f.porcentaje)
        const faltan = Math.max(0, f.matriculados - f.respondieron)
        return (
          <li key={`${f.idDocente ?? ''}-${f.nombre}`} className="cobertura__fila">
            <div className="cobertura__nombre">
              <strong>{f.nombre}</strong>
              <span>
                {f.respondieron} de {f.matriculados} respondieron{faltan ? ` · faltan ${faltan}` : ''}
              </span>
            </div>
            <div className="cobertura__medida">
              <span className="cobertura__riel" aria-hidden="true">
                <span className={`cobertura__relleno cobertura__relleno--${tono}`} style={{ width: `${Math.min(100, Number(f.porcentaje))}%` }} />
              </span>
              <span className={`cobertura__valor cobertura__valor--${tono}`}>{porcentaje(f.porcentaje)}</span>
            </div>
            {conEnlace && f.idDocente && (
              <BotonAccion tipo="ver" nombre={f.nombre} a={`/docentes/${f.idDocente}`} />
            )}
          </li>
        )
      })}
    </ul>
  )
}

function Resumen({ datos }) {
  const { total } = datos
  const faltan = Math.max(0, total.matriculados - total.respondieron)
  const tono = tonoCobertura(total.porcentaje)
  const angulo = Math.min(100, Number(total.porcentaje)) * 3.6
  return (
    <section className="matricula__resumen" aria-label="Resumen de la cobertura">
      <div className={`matricula__medidor matricula__medidor--${tono}`} style={{ '--angulo': `${angulo}deg` }}>
        <span>
          <strong>{porcentaje(total.porcentaje)}</strong>
          respondieron
        </span>
      </div>
      <div className="matricula__cifras">
        <div>
          <Users size={18} aria-hidden="true" />
          <span>Matrículas</span>
          <strong>{total.matriculados}</strong>
        </div>
        <div>
          <UserCheck size={18} aria-hidden="true" />
          <span>Respondieron</span>
          <strong>{total.respondieron}</strong>
        </div>
        <div>
          <UserX size={18} aria-hidden="true" />
          <span>Faltan</span>
          <strong>{faltan}</strong>
        </div>
      </div>
      <p className="matricula__privacidad">
        <ShieldCheck size={18} aria-hidden="true" />
        Solo se cuentan respuestas. Ninguna pantalla muestra quién respondió ni qué respondió.
      </p>
    </section>
  )
}

export default function Matricula() {
  const { usuario } = useSesion()
  const [pagina, setPagina] = useState(0)
  const [busqueda, setBusqueda] = useState('')
  const [orden, setOrden] = useState('menor')
  const cargar = useCallback(() => obtenerCobertura(pagina), [pagina])
  const cobertura = useFetch(cargar, [cargar])
  const datos = cobertura.datos

  const docentes = useMemo(() => {
    const termino = normalizarTexto(busqueda.trim())
    const lista = (datos?.docentes.contenido ?? []).filter((d) => !termino || normalizarTexto(d.nombre).includes(termino))
    return [...lista].sort((a, b) =>
      orden === 'nombre' ? a.nombre.localeCompare(b.nombre) : orden === 'menor' ? a.porcentaje - b.porcentaje : b.porcentaje - a.porcentaje,
    )
  }, [datos, busqueda, orden])

  return (
    <>
      <Encabezado
        miga={`${ETIQUETA_ROL[usuario.rol]} · Matrícula y cobertura`}
        titulo="Matrícula y cobertura"
        subtitulo="Quién debe evaluar a cada profesor y cuántos ya respondieron. Nunca se ve quién respondió qué."
      />
      <div className="pagina">
        {cobertura.cargando && !datos && <Cargando texto="Cargando la cobertura" bloques={1} />}
        {cobertura.error && (
          <EstadoVista tipo="error" titulo="No fue posible cargar la cobertura" mensaje={cobertura.error.message}>
            <Boton icono={RefreshCw} onClick={cobertura.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}
        {datos && <Resumen datos={datos} />}

        <Importacion alImportar={cobertura.recargar} />

        {datos && (
          <div className="matricula__rejilla">
            <Panel
              titulo={`Cobertura por docente · ${datos.periodo.nombre}`}
              descripcion={`${datos.total.respondieron} de ${datos.total.matriculados} (${porcentaje(datos.total.porcentaje)})`}
              icono={Users}
              tono="turquesa"
            >
              {datos.docentes.contenido.length === 0 ? (
                <p className="campo__ayuda">Aún no hay matrícula en este periodo.</p>
              ) : (
                <>
                  <div className="matricula__herramientas">
                    <label className="matricula__buscador">
                      <Search size={16} aria-hidden="true" />
                      <span className="solo-lectores">Buscar docente</span>
                      <input type="search" placeholder="Buscar docente" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
                    </label>
                    <label className="matricula__orden">
                      <span>Ordenar</span>
                      <select className="campo__control" value={orden} onChange={(e) => setOrden(e.target.value)}>
                        <option value="menor">Menor cobertura primero</option>
                        <option value="mayor">Mayor cobertura primero</option>
                        <option value="nombre">Por nombre</option>
                      </select>
                    </label>
                  </div>
                  {docentes.length === 0 ? (
                    <p className="campo__ayuda">Ningún docente coincide con la búsqueda.</p>
                  ) : (
                    <TablaCobertura filas={docentes} primeraColumna="Docente" conEnlace />
                  )}
                  {datos.docentes.totalPaginas > 1 && (
                    <div className="matricula__paginas">
                      <Boton variante="contorno" disabled={pagina === 0} onClick={() => setPagina((p) => p - 1)}>
                        Anterior
                      </Boton>
                      <span>
                        Página {pagina + 1} de {datos.docentes.totalPaginas}
                      </span>
                      <Boton
                        variante="contorno"
                        disabled={pagina + 1 >= datos.docentes.totalPaginas}
                        onClick={() => setPagina((p) => p + 1)}
                      >
                        Siguiente
                      </Boton>
                    </div>
                  )}
                </>
              )}
            </Panel>
            {datos.programas.length > 0 && (
              <Panel titulo="Por programa" descripcion="Suma de las matrículas de cada programa." icono={FileSpreadsheet} tono="violeta">
                <TablaCobertura filas={datos.programas} primeraColumna="Programa" />
              </Panel>
            )}
          </div>
        )}
      </div>
    </>
  )
}
