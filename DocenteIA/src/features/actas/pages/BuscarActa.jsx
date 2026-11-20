import { Download, ExternalLink, Eye, FileSignature, Search } from 'lucide-react'
import { useCallback, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Modal } from '../../../shared/components/ui/Modal/Modal.jsx'
import { Paginacion } from '../../../shared/components/ui/Paginacion/Paginacion.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETIQUETA_ROL } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { usePaginacion } from '../../../shared/hooks/usePaginacion.js'
import { formatearFecha, formatearFechaHora } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { buscarActaPorCodigo, descargarPdfActa, listarRegistroActas } from '../services/actasService.js'
import './VerificarActa.css'

const TIPOS = [
  { clave: 'TODAS', etiqueta: 'Todas' },
  { clave: 'COMPROMISO', etiqueta: 'De compromiso' },
  { clave: 'RETROALIMENTACION', etiqueta: 'De retroalimentación' },
]
// Un código completo, tal como sale en el PDF.
const CODIGO_COMPLETO = /^DOC-[0-9A-F]{12}$/

const tipoDe = (acta) => (acta.nombreActa.includes('compromiso') ? 'COMPROMISO' : 'RETROALIMENTACION')
const normalizar = (texto) =>
  (texto ?? '')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()

function guardarArchivo(blob, nombre) {
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombre
  enlace.click()
  URL.revokeObjectURL(url)
}

/** Descarga el PDF firmado; el servidor deja constancia de la lectura. */
function useDescarga() {
  const [error, setError] = useState(null)
  const [descargando, setDescargando] = useState(null)
  async function descargar(acta) {
    setError(null)
    setDescargando(acta.idActa)
    try {
      guardarArchivo(await descargarPdfActa(acta.idActa), `acta-${acta.codigo}.pdf`)
    } catch (errorApi) {
      setError(errorApi)
    } finally {
      setDescargando(null)
    }
  }
  return { descargar, descargando, error, limpiar: () => setError(null) }
}

/**
 * Archivo de actas firmadas para la administración y la vicerrectoría: un buscador (docente,
 * programa o código del PDF), la lista y, al abrir una, quién la firmó y su descarga. Nunca la nota.
 */
export default function BuscarActa() {
  const { usuario } = useSesion()
  const [parametros, setParametros] = useSearchParams()
  const abierta = parametros.get('codigo')
  const descarga = useDescarga()

  function abrir(codigo) {
    descarga.limpiar()
    setParametros((actuales) => {
      const nuevos = new URLSearchParams(actuales)
      if (codigo) nuevos.set('codigo', codigo)
      else nuevos.delete('codigo')
      return nuevos
    })
  }

  return (
    <>
      <Encabezado
        miga={`${ETIQUETA_ROL[usuario.rol]} · Actas firmadas`}
        titulo="Actas firmadas"
        subtitulo="Todas las actas de compromiso y de retroalimentación que ya firmaron el docente y quien lo evaluó. Ábralas para ver quién firmó o descárguelas en PDF."
      />
      <div className="pagina">
        <Registro alAbrir={abrir} descarga={descarga} />
      </div>
      {abierta && <DetalleActa codigo={abierta} descarga={descarga} alCerrar={() => abrir(null)} />}
    </>
  )
}

function Registro({ alAbrir, descarga }) {
  const cargar = useCallback(() => listarRegistroActas(), [])
  const registro = useFetch(cargar, [cargar])
  const [texto, setTexto] = useState('')
  const [tipo, setTipo] = useState('TODAS')
  const [conReemplazadas, setConReemplazadas] = useState(false)

  const todas = useMemo(() => registro.datos ?? [], [registro.datos])
  const vigentes = todas.filter((a) => a.estado === 'FIRMADA')
  const buscado = texto.trim()
  // Con un código completo se busca esa acta aunque haya sido reemplazada.
  const porCodigo = CODIGO_COMPLETO.test(buscado.toUpperCase())

  const filas = useMemo(() => {
    const clave = normalizar(buscado)
    return todas.filter(
      (a) =>
        (conReemplazadas || porCodigo || a.estado === 'FIRMADA') &&
        (tipo === 'TODAS' || tipoDe(a) === tipo) &&
        (!clave || normalizar(`${a.nombreDocente} ${a.codigo} ${a.programa ?? ''} ${a.escuela}`).includes(clave)),
    )
  }, [todas, buscado, tipo, conReemplazadas, porCodigo])
  const paginacion = usePaginacion(filas)

  const cuenta = (clave) => vigentes.filter((a) => clave === 'TODAS' || tipoDe(a) === clave).length

  return (
    <Panel
      titulo="Registro de actas"
      descripcion={
        registro.datos
          ? `${vigentes.length} actas vigentes · ${todas.length - vigentes.length} reemplazadas por una corrección`
          : undefined
      }
      icono={FileSignature}
      tono="rosa"
    >
      <div className="registro-actas__buscador">
        <label htmlFor="buscar-acta" className="rotulo rotulo--campo">
          Buscar acta
        </label>
        <div className="registro-actas__busqueda">
          <Search size={18} aria-hidden="true" />
          <input
            id="buscar-acta"
            className="campo__control"
            type="search"
            placeholder="Nombre del docente, programa o código (DOC-…)"
            value={texto}
            autoComplete="off"
            onChange={(e) => setTexto(e.target.value)}
            aria-describedby="buscar-acta-ayuda"
          />
        </div>
        <span id="buscar-acta-ayuda" className="campo__ayuda">
          El código del acta está en la esquina superior derecha del PDF.
        </span>
      </div>

      <div className="registro-actas__filtros">
        <div className="registro-actas__tipos" role="group" aria-label="Tipo de acta">
          {TIPOS.map((t) => (
            <button
              key={t.clave}
              type="button"
              className={`registro-actas__tipo${tipo === t.clave ? ' registro-actas__tipo--activo' : ''}`}
              aria-pressed={tipo === t.clave}
              onClick={() => setTipo(t.clave)}
            >
              {t.etiqueta}
              {registro.datos && <span className="registro-actas__cuenta">{cuenta(t.clave)}</span>}
            </button>
          ))}
        </div>
        <label className="registro-actas__casilla">
          <input type="checkbox" checked={conReemplazadas} onChange={(e) => setConReemplazadas(e.target.checked)} />
          Mostrar también las reemplazadas
        </label>
      </div>

      {descarga.error && <ErrorApi error={descarga.error} />}
      {registro.cargando && <Cargando texto="Cargando actas" bloques={3} />}
      {registro.error && <ErrorApi error={registro.error} />}
      {!registro.cargando && !registro.error && filas.length === 0 && (
        <Aviso
          tipo="info"
          titulo={buscado ? `Ningún acta coincide con «${buscado}»` : 'Todavía no hay actas firmadas'}
        >
          {buscado && <p>Revise lo escrito o, si busca por código, cópielo tal como aparece en el PDF.</p>}
        </Aviso>
      )}
      {filas.length > 0 && (
        <>
          <div className="tabla-contenedor">
            <table className="tabla registro-actas__tabla">
              <thead>
                <tr>
                  <th scope="col">Docente</th>
                  <th scope="col">Tipo de acta</th>
                  <th scope="col" className="registro-actas__opcional">
                    Periodo
                  </th>
                  <th scope="col" className="registro-actas__opcional">
                    Firmada
                  </th>
                  <th scope="col">Estado</th>
                  <th scope="col">
                    <span className="solo-lectores">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {paginacion.visibles.map((a) => (
                  <tr key={a.idActa}>
                    <td>
                      <strong>{a.nombreDocente}</strong>
                      <span className="registro-actas__secundario">{a.programa ?? a.escuela}</span>
                      <span className="registro-actas__codigo">{a.codigo}</span>
                    </td>
                    <td>
                      <span className={`pildora pildora--${tipoDe(a) === 'COMPROMISO' ? 'coral' : 'cielo'}`}>
                        {tipoDe(a) === 'COMPROMISO' ? 'Compromiso' : 'Retroalimentación'}
                      </span>
                    </td>
                    <td className="registro-actas__opcional">{a.periodo}</td>
                    <td className="registro-actas__opcional">{a.fechaFirma ? formatearFecha(a.fechaFirma) : '—'}</td>
                    <td>
                      <span className={`pildora pildora--${a.estado === 'FIRMADA' ? 'exito' : 'neutro'}`}>
                        {a.estado === 'FIRMADA' ? 'Vigente' : 'Reemplazada'}
                      </span>
                    </td>
                    <td>
                      <div className="registro-actas__acciones">
                        <Boton
                          variante="contorno"
                          icono={Eye}
                          aria-label={`Ver el acta de ${a.nombreDocente}`}
                          onClick={() => alAbrir(a.codigo)}
                        >
                          Ver
                        </Boton>
                        <Boton
                          variante="contorno"
                          icono={Download}
                          aria-label={`Descargar el acta de ${a.nombreDocente}`}
                          disabled={descarga.descargando === a.idActa}
                          onClick={() => descarga.descargar(a)}
                        >
                          {descarga.descargando === a.idActa ? 'Descargando…' : 'PDF'}
                        </Boton>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Paginacion {...paginacion} etiqueta="actas" />
        </>
      )}
    </Panel>
  )
}

/** Ventana con los datos del acta: de quién es, quién firmó y cuándo; descarga y verificación. */
function DetalleActa({ codigo, descarga, alCerrar }) {
  const cargar = useCallback(() => buscarActaPorCodigo(codigo), [codigo])
  const { datos, error, cargando } = useFetch(cargar, [cargar])
  const vigente = datos?.estado === 'FIRMADA'

  return (
    <Modal
      titulo={datos?.nombreActa ?? 'Acta'}
      descripcion={datos ? `Código ${datos.codigo} · Periodo ${datos.periodo}` : codigo}
      icono={FileSignature}
      onCerrar={alCerrar}
    >
      {cargando && <Cargando texto="Cargando el acta" bloques={2} />}
      {error?.status === 404 && <Aviso tipo="alerta" titulo={`Ningún acta tiene el código ${codigo}`} />}
      {error && error.status !== 404 && <ErrorApi error={error} />}
      {datos && (
        <div className="registro-actas__detalle">
          <Aviso
            tipo={vigente ? 'exito' : 'alerta'}
            titulo={vigente ? 'Acta vigente' : 'Reemplazada por una corrección'}
          >
            {!vigente && <p>Esta versión ya no es válida; la vigente es la más reciente del mismo docente.</p>}
          </Aviso>
          <dl className="verificacion__datos">
            <dt>Docente</dt>
            <dd>
              {datos.nombreDocente}
              <span className="registro-actas__secundario">{datos.correoDocente}</span>
            </dd>
            {datos.programa && (
              <>
                <dt>Programa</dt>
                <dd>{datos.programa}</dd>
              </>
            )}
            <dt>Escuela</dt>
            <dd>{datos.escuela}</dd>
            {datos.firmas.map((firma, i) => (
              <FirmaDetalle key={`${firma.correo}-${firma.fecha}`} firma={firma} titulo={`Firma ${i + 1}`} />
            ))}
          </dl>
          {descarga.error && <ErrorApi error={descarga.error} />}
          <div className="registro-actas__pie">
            <Boton icono={Download} onClick={() => descarga.descargar(datos)} disabled={descarga.descargando === datos.idActa}>
              {descarga.descargando === datos.idActa ? 'Descargando…' : 'Descargar PDF'}
            </Boton>
            <Link
              className="boton boton--contorno"
              to={`/verificar-acta/${encodeURIComponent(datos.codigo)}`}
              target="_blank"
            >
              <ExternalLink size={16} aria-hidden="true" /> Página de verificación
            </Link>
            <Boton variante="enlace" onClick={alCerrar}>
              Cerrar
            </Boton>
          </div>
          <details className="registro-actas__huella">
            <summary>Huella digital del contenido (SHA-256)</summary>
            <code>{datos.hashContenido}</code>
          </details>
        </div>
      )}
    </Modal>
  )
}

function FirmaDetalle({ firma, titulo }) {
  return (
    <>
      <dt>{titulo}</dt>
      <dd>
        <strong>{firma.nombre}</strong>
        <span className="registro-actas__secundario">{firma.cargo}</span>
        <span className="registro-actas__secundario">Firmó el {formatearFechaHora(firma.fecha)}</span>
      </dd>
    </>
  )
}
