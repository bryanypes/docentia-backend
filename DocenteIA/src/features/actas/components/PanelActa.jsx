import { CalendarClock, Download, FileSignature, ListPlus, Plus, RefreshCw } from 'lucide-react'
import { useCallback, useRef, useState } from 'react'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { BotonAccion } from '../../../shared/components/ui/BotonAccion/BotonAccion.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { ChipCategoria } from '../../../shared/components/ui/EtiquetaNivel/EtiquetaNivel.jsx'
import { Modal } from '../../../shared/components/ui/Modal/Modal.jsx'
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
import { ModalFirma } from './ModalFirma.jsx'
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

const SUGERENCIAS = [
  'Actualizar el plan de asignatura con resultados de aprendizaje',
  'Retroalimentar cada evaluación en máximo ocho días',
  'Participar en el curso de formación docente del periodo',
  'Incorporar una estrategia didáctica activa por unidad',
]

/** Fecha AAAA-MM-DD a n meses de hoy, para los plazos rápidos. */
function enMeses(meses) {
  const fecha = new Date()
  fecha.setMonth(fecha.getMonth() + meses)
  return fecha.toISOString().slice(0, 10)
}

/** Modal para agregar un compromiso: qué se compromete el docente y para cuándo. */
function ModalCompromiso({ acta, ejecutar, ocupado, onCerrar }) {
  const [descripcion, setDescripcion] = useState('')
  const [fechaLimite, setFechaLimite] = useState('')
  // Se calculan al abrir el modal: hoy y los plazos rápidos no cambian mientras está abierto.
  const [hoy] = useState(() => enMeses(0))
  const [plazos] = useState(() => [
    [enMeses(1), 'En 1 mes'],
    [enMeses(3), 'En 3 meses'],
    [enMeses(6), 'En 6 meses'],
  ])
  const idDescripcion = `compromiso-${acta.idActa}`
  const idPlazo = `plazo-${acta.idActa}`
  const listo = descripcion.trim().length >= 5 && fechaLimite

  async function guardar(evento) {
    evento.preventDefault()
    if (!listo) return
    const hecho = await ejecutar(() => agregarCompromiso(acta.idActa, { descripcion: descripcion.trim(), fechaLimite }))
    if (hecho) onCerrar()
  }

  return (
    <Modal
      titulo="Agregar compromiso"
      descripcion="Una acción concreta que el docente se compromete a cumplir, con su plazo."
      icono={ListPlus}
      ancho="pequeno"
      onCerrar={onCerrar}
    >
      <form className="compromiso-nuevo" onSubmit={guardar}>
        <Campo id={idDescripcion} etiqueta="Compromiso" ayuda="Empieza con un verbo: actualizar, participar, incorporar…">
          <textarea
            id={idDescripcion}
            className="campo__control"
            rows={3}
            maxLength={500}
            required
            value={descripcion}
            onChange={(e) => setDescripcion(e.target.value)}
          />
        </Campo>
        <div className="compromiso-nuevo__sugerencias" role="group" aria-label="Sugerencias">
          {SUGERENCIAS.map((texto) => (
            <button key={texto} type="button" className="compromiso-nuevo__sugerencia" onClick={() => setDescripcion(texto)}>
              {texto}
            </button>
          ))}
        </div>
        <Campo id={idPlazo} etiqueta="Plazo">
          <input
            id={idPlazo}
            type="date"
            className="campo__control"
            required
            min={hoy}
            value={fechaLimite}
            onChange={(e) => setFechaLimite(e.target.value)}
          />
        </Campo>
        <div className="compromiso-nuevo__plazos" role="group" aria-label="Plazos rápidos">
          {plazos.map(([fecha, texto]) => (
            <button
              key={fecha}
              type="button"
              className={`compromiso-nuevo__plazo${fechaLimite === fecha ? ' compromiso-nuevo__plazo--activo' : ''}`}
              aria-pressed={fechaLimite === fecha}
              onClick={() => setFechaLimite(fecha)}
            >
              {texto}
            </button>
          ))}
        </div>
        <div className="compromiso-nuevo__acciones">
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton type="submit" icono={Plus} disabled={ocupado || !listo}>
            Guardar compromiso
          </Boton>
        </div>
      </form>
    </Modal>
  )
}

function Compromisos({ acta, editable, ejecutar, ocupado }) {
  const [agregando, setAgregando] = useState(false)
  const faltan = editable && acta.requiereCompromisos && acta.compromisos.length === 0
  const titulo = acta.categoria?.codigo === 'BAJO' ? 'Plan de Mejoramiento Profesoral' : 'Compromisos'

  return (
    <section className="compromisos">
      <div className="compromisos__cabecera">
        <h3 className="acta__subtitulo">
          {titulo}
          <span className="compromisos__contador">{acta.compromisos.length}</span>
        </h3>
        {editable && (
          <Boton icono={Plus} disabled={ocupado} onClick={() => setAgregando(true)}>
            Agregar compromiso
          </Boton>
        )}
      </div>
      {acta.compromisos.length === 0 ? (
        <div className={`compromisos__vacio${faltan ? ' compromisos__vacio--requerido' : ''}`}>
          <ListPlus size={22} aria-hidden="true" />
          <div>
            <strong>{faltan ? 'Esta acta necesita al menos un compromiso' : 'Sin compromisos registrados'}</strong>
            <span>
              {editable
                ? 'Pulsa «Agregar compromiso» para escribir la acción y su plazo.'
                : 'La coordinación los agrega en la reunión.'}
            </span>
          </div>
        </div>
      ) : (
        <ol className="compromisos__lista">
          {acta.compromisos.map((c, indice) => (
            <li key={c.idCompromiso} className="compromisos__item">
              <span className="compromisos__numero" aria-hidden="true">
                {indice + 1}
              </span>
              <div className="compromisos__texto">
                <strong>{c.descripcion}</strong>
                <span>
                  <CalendarClock size={14} aria-hidden="true" /> Plazo {formatearFecha(c.fechaLimite)}
                </span>
              </div>
              {editable && (
                <BotonAccion
                  tipo="eliminar"
                  texto="Quitar"
                  nombre={c.descripcion}
                  onClick={() => ejecutar(() => quitarCompromiso(acta.idActa, c.idCompromiso))}
                />
              )}
            </li>
          ))}
        </ol>
      )}
      {agregando && <ModalCompromiso acta={acta} ejecutar={ejecutar} ocupado={ocupado} onCerrar={() => setAgregando(false)} />}
    </section>
  )
}

function Firma({ acta, usuario, ejecutar, ocupado, error }) {
  const [firmando, setFirmando] = useState(false)
  const puedeFirmar =
    acta.estado === 'BORRADOR' &&
    (usuario.rol === ROLES.COORDINADOR || usuario.rol === ROLES.DOCENTE) &&
    !acta.firmas.some((firma) => firma.rol === usuario.rol)

  async function firmar(trazo) {
    if (await ejecutar(() => firmarActa(acta.idActa, trazo))) setFirmando(false)
  }

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
              {firma.conTrazo && <span className="pildora pildora--rosa">Firma dibujada</span>}
            </li>
          ))}
        </ul>
      )}
      {puedeFirmar && (
        <div className="acta__firmar">
          <Boton icono={FileSignature} disabled={ocupado} onClick={() => setFirmando(true)}>
            Firmar acta
          </Boton>
          <span className="campo__ayuda">Dibujas tu firma aquí mismo; queda impresa en el PDF.</span>
        </div>
      )}
      {firmando && (
        <ModalFirma
          nombre={usuario.nombreCompleto}
          tipoActa={acta.tipo ? ETIQUETA_TIPO_ACTA[acta.tipo] : null}
          enviando={ocupado}
          error={error}
          onFirmar={firmar}
          onCerrar={() => setFirmando(false)}
        />
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

function DetalleActa({ acta, usuario, rolCorrector, ejecutar, ocupado, error, onDescargar }) {
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
      <Firma acta={acta} usuario={usuario} ejecutar={ejecutar} ocupado={ocupado} error={error} />
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
  const propsDetalle = { usuario, rolCorrector, ejecutar, ocupado, error, onDescargar: descargar }

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
