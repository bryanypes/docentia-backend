import { Download, FileSignature, Plus, RefreshCw, Trash2 } from 'lucide-react'
import { useCallback, useRef, useState } from 'react'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { ChipCategoria } from '../../../shared/components/ui/EtiquetaNivel/EtiquetaNivel.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETIQUETA_ROL, ROLES } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFecha, formatearFechaHora, formatearNota } from '../../../shared/utils/formatters.js'
import { useSesion } from '../../auth/sesionContext.js'
import { obtenerConfiguracionEvaluacion } from '../../kpis/services/configuracionService.js'
import { ETIQUETA_TIPO_ACTA } from '../../proceso/estados.js'
import {
  agregarCompromiso,
  cambiarTipoActa,
  corregirActa,
  crearActa,
  descargarPdfActa,
  firmarActa,
  listarActas,
  quitarCompromiso,
} from '../services/actasService.js'
import './PanelActa.css'

const ESTADO_ACTA = {
  BORRADOR: { etiqueta: 'Borrador', clase: 'info' },
  FIRMADA: { etiqueta: 'Firmada', clase: 'exito' },
  ANULADA: { etiqueta: 'Anulada', clase: 'neutro' },
}

function guardarArchivo(blob, nombre) {
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombre
  enlace.click()
  URL.revokeObjectURL(url)
}

function Compromisos({ acta, editable, ejecutar, ocupado }) {
  const [descripcion, setDescripcion] = useState('')
  const [fechaLimite, setFechaLimite] = useState('')

  async function agregar(evento) {
    evento.preventDefault()
    const hecho = await ejecutar(() => agregarCompromiso(acta.idActa, { descripcion: descripcion.trim(), fechaLimite }))
    if (hecho) {
      setDescripcion('')
      setFechaLimite('')
    }
  }

  return (
    <section>
      <h3 className="acta__subtitulo">{acta.categoria?.codigo === 'BAJO' ? 'Plan de Mejoramiento Profesoral' : 'Compromisos'}</h3>
      {acta.compromisos.length === 0 ? (
        <p className="campo__ayuda">Sin compromisos registrados.</p>
      ) : (
        <ul className="acta__compromisos">
          {acta.compromisos.map((c) => (
            <li key={c.idCompromiso}>
              <span>
                {c.descripcion} · Plazo {formatearFecha(c.fechaLimite)}
              </span>
              {editable && (
                <Boton
                  variante="contorno"
                  icono={Trash2}
                  aria-label={`Quitar ${c.descripcion}`}
                  onClick={() => ejecutar(() => quitarCompromiso(acta.idActa, c.idCompromiso))}
                />
              )}
            </li>
          ))}
        </ul>
      )}
      {editable && (
        <form className="acta__formulario" onSubmit={agregar}>
          <Campo id={`compromiso-${acta.idActa}`} etiqueta="Compromiso">
            <input
              id={`compromiso-${acta.idActa}`}
              className="campo__control"
              maxLength={500}
              required
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
            />
          </Campo>
          <Campo id={`plazo-${acta.idActa}`} etiqueta="Plazo">
            <input
              id={`plazo-${acta.idActa}`}
              type="date"
              className="campo__control"
              required
              value={fechaLimite}
              onChange={(e) => setFechaLimite(e.target.value)}
            />
          </Campo>
          <Boton type="submit" variante="contorno" icono={Plus} disabled={ocupado}>
            Agregar compromiso
          </Boton>
        </form>
      )}
    </section>
  )
}

function Firma({ acta, usuario, ejecutar, ocupado }) {
  const [confirmado, setConfirmado] = useState(false)
  const puedeFirmar =
    acta.estado === 'BORRADOR' &&
    (usuario.rol === ROLES.COORDINADOR || usuario.rol === ROLES.DOCENTE) &&
    !acta.firmas.some((firma) => firma.rol === usuario.rol)
  const idConfirmacion = `confirmar-${acta.idActa}`

  return (
    <section>
      <h3 className="acta__subtitulo">Firmas</h3>
      {acta.firmas.length === 0 ? (
        <p className="campo__ayuda">Aún no firma nadie. Firman el docente y la coordinación.</p>
      ) : (
        <ul className="acta__firmas">
          {acta.firmas.map((firma) => (
            <li key={firma.rol}>
              {ETIQUETA_ROL[firma.rol]}: {firma.nombre} · {formatearFechaHora(firma.fecha)}
            </li>
          ))}
        </ul>
      )}
      {puedeFirmar && (
        <div className="acta__firmar">
          <label htmlFor={idConfirmacion}>
            <input
              id={idConfirmacion}
              type="checkbox"
              checked={confirmado}
              onChange={(e) => setConfirmado(e.target.checked)}
            />{' '}
            Leí el acta y entiendo que firmar es irreversible.
          </label>
          <Boton icono={FileSignature} disabled={!confirmado || ocupado} onClick={() => ejecutar(() => firmarActa(acta.idActa))}>
            Firmar acta
          </Boton>
        </div>
      )}
    </section>
  )
}

function Correccion({ acta, ejecutar, ocupado }) {
  const [motivo, setMotivo] = useState('')
  const id = `motivo-correccion-${acta.idActa}`

  async function corregir(evento) {
    evento.preventDefault()
    if (await ejecutar(() => corregirActa(acta.idActa, motivo.trim()))) setMotivo('')
  }

  return (
    <form className="acta__formulario" onSubmit={corregir}>
      <Campo id={id} etiqueta="Motivo de la corrección" ayuda="Crea una versión nueva y anula esta. La firmada no cambia.">
        <textarea
          id={id}
          className="campo__control"
          rows={2}
          maxLength={500}
          required
          value={motivo}
          onChange={(e) => setMotivo(e.target.value)}
        />
      </Campo>
      <Boton type="submit" variante="contorno" disabled={ocupado}>
        Corregir con una versión nueva
      </Boton>
    </form>
  )
}

/** Selector del tipo de acta para la coordinación, mientras nadie haya firmado. */
function TipoActa({ acta, ejecutar, ocupado }) {
  const id = `tipo-acta-${acta.idActa}`
  return (
    <Campo id={id} etiqueta="Tipo de acta" ayuda="Con una nota bajo el umbral solo cabe el acta de compromiso.">
      <select
        id={id}
        className="campo__control"
        value={acta.tipo}
        disabled={ocupado}
        onChange={(e) => ejecutar(() => cambiarTipoActa(acta.idActa, e.target.value))}
      >
        <option value="RETROALIMENTACION">{ETIQUETA_TIPO_ACTA.RETROALIMENTACION}</option>
        <option value="COMPROMISO">{ETIQUETA_TIPO_ACTA.COMPROMISO}</option>
      </select>
    </Campo>
  )
}

function DetalleActa({ acta, usuario, rolCorrector, ejecutar, ocupado, onDescargar }) {
  const estado = ESTADO_ACTA[acta.estado]
  const editable = acta.estado === 'BORRADOR' && usuario.rol === ROLES.COORDINADOR && acta.firmas.length === 0
  const umbral = formatearNota(acta.umbralCompromisos)

  return (
    <div className="acta">
      <div className="acta__resumen">
        <strong>
          {acta.tipo ? `${ETIQUETA_TIPO_ACTA[acta.tipo]} · ` : ''}Periodo {acta.periodo.nombre} · versión {acta.version}
        </strong>
        <span className={`pildora pildora--${estado.clase}`}>{estado.etiqueta}</span>
        {acta.tipo && (
          <span className={`pildora pildora--${acta.tipo === 'COMPROMISO' ? 'rosa' : 'turquesa'}`}>
            {acta.tipo === 'COMPROMISO' ? 'Con compromisos' : 'Retroalimentación recibida'}
          </span>
        )}
        {acta.categoria ? (
          <>
            <span>Nota {formatearNota(acta.promedio)}</span>
            <ChipCategoria categoria={acta.categoria} />
          </>
        ) : (
          <span>Resultado pendiente</span>
        )}
      </div>
      {acta.categoria && <p className="acta__accion">Acción institucional: {acta.categoria.accionInstitucional}</p>}
      {acta.motivoCorreccion && <p className="acta__accion">Motivo de la corrección: {acta.motivoCorreccion}</p>}
      {editable && acta.tipo && <TipoActa acta={acta} ejecutar={ejecutar} ocupado={ocupado} />}
      {acta.estado === 'BORRADOR' && acta.categoria && (
        <Aviso
          tipo="info"
          titulo={
            acta.requiereCompromisos
              ? Number(acta.promedio) < Number(acta.umbralCompromisos)
                ? `Nota menor a ${umbral}`
                : 'Acta de compromiso'
              : `Nota de ${umbral} o más`
          }
        >
          {acta.requiereCompromisos
            ? 'El acta necesita al menos un compromiso con plazo antes de firmarse.'
            : 'Es la constancia de que el docente recibió la retroalimentación: basta con la confirmación de lectura.'}
        </Aviso>
      )}
      <Compromisos acta={acta} editable={editable} ejecutar={ejecutar} ocupado={ocupado} />
      <Firma acta={acta} usuario={usuario} ejecutar={ejecutar} ocupado={ocupado} />
      {acta.estado === 'FIRMADA' && (
        <>
          <p className="acta__sello">
            Código {acta.codigoVerificacion}
            <br />
            SHA-256 {acta.hashContenido}
          </p>
          <Boton variante="contorno" icono={Download} onClick={() => onDescargar(acta)}>
            Descargar PDF
          </Boton>
          {usuario.rol === rolCorrector && <Correccion acta={acta} ejecutar={ejecutar} ocupado={ocupado} />}
        </>
      )}
    </div>
  )
}

/**
 * Acta de retroalimentación del docente: la coordinación la genera y agrega compromisos, firman las
 * dos partes y queda el PDF. El backend repite todas las reglas; aquí solo se ocultan acciones.
 * Sin `idDocente` muestra las del docente autenticado.
 */
export function PanelActa({ idDocente }) {
  const { usuario } = useSesion()
  const cargar = useCallback(() => listarActas(idDocente), [idDocente])
  const actas = useFetch(cargar, [cargar])
  const configuracion = useFetch(obtenerConfiguracionEvaluacion)
  const [error, setError] = useState(null)
  const [ocupado, setOcupado] = useState(false)
  // La referencia frena el doble clic antes de que React vuelva a dibujar.
  const enCurso = useRef(false)

  async function ejecutar(accion) {
    if (enCurso.current) return false
    enCurso.current = true
    setOcupado(true)
    setError(null)
    try {
      await accion()
      actas.recargar()
      return true
    } catch (errorApi) {
      setError(errorApi)
      return false
    } finally {
      enCurso.current = false
      setOcupado(false)
    }
  }

  async function descargar(acta) {
    setError(null)
    try {
      guardarArchivo(await descargarPdfActa(acta.idActa), `acta-${acta.codigoVerificacion}.pdf`)
    } catch (errorApi) {
      setError(errorApi)
    }
  }

  const [tipoNueva, setTipoNueva] = useState('')
  const lista = actas.datos ?? []
  const [vigente, ...anteriores] = lista
  const rolCorrector = configuracion.datos?.rolCorrectorActas
  const propsDetalle = { usuario, rolCorrector, ejecutar, ocupado, onDescargar: descargar }

  return (
    <Panel titulo="Acta de la reunión" descripcion="Acta de compromiso o de retroalimentación; se firma en la reunión." icono={FileSignature} tono="rosa">
      {actas.cargando && !actas.datos && <Cargando texto="Cargando el acta" bloques={1} />}
      {actas.error && (
        <EstadoVista tipo="error" titulo="No fue posible cargar el acta" mensaje={actas.error.message}>
          <Boton icono={RefreshCw} onClick={actas.recargar}>Intentar de nuevo</Boton>
        </EstadoVista>
      )}
      <ErrorApi error={error} />
      {actas.datos && lista.length === 0 && (
        <>
          <p className="campo__ayuda">
            {usuario.rol === ROLES.DOCENTE
              ? 'Tu acta aparece en la reunión de retroalimentación, cuando la coordinación registra tu asistencia. Ahí mismo la firmas.'
              : 'Todavía no hay un acta en este periodo.'}
          </p>
          {usuario.rol === ROLES.COORDINADOR && (
            <div className="formulario-simple">
              <Campo id={`tipo-nueva-${idDocente}`} etiqueta="Tipo de acta">
                <select
                  id={`tipo-nueva-${idDocente}`}
                  className="campo__control"
                  value={tipoNueva}
                  onChange={(e) => setTipoNueva(e.target.value)}
                >
                  <option value="">Según la nota</option>
                  <option value="RETROALIMENTACION">{ETIQUETA_TIPO_ACTA.RETROALIMENTACION}</option>
                  <option value="COMPROMISO">{ETIQUETA_TIPO_ACTA.COMPROMISO}</option>
                </select>
              </Campo>
              <Boton
                disabled={ocupado}
                onClick={() => ejecutar(() => (tipoNueva ? crearActa(idDocente, tipoNueva) : crearActa(idDocente)))}
              >
                Generar acta
              </Boton>
            </div>
          )}
        </>
      )}
      {vigente && <DetalleActa key={`${vigente.idActa}-${vigente.estado}`} acta={vigente} {...propsDetalle} />}
      {anteriores.length > 0 && (
        <details className="acta__historial">
          <summary>Historial de versiones ({anteriores.length})</summary>
          {anteriores.map((acta) => (
            <DetalleActa key={acta.idActa} acta={acta} {...propsDetalle} />
          ))}
        </details>
      )}
    </Panel>
  )
}
