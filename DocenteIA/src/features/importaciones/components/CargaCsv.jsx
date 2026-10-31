import { FileSearch, Save, Upload } from 'lucide-react'
import { useId, useState } from 'react'
import {
  usePeriodoActivo,
  useVentanaEtapa,
} from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETAPAS, ETIQUETA_ORIGEN_MAPEO } from '../../../shared/constants/dominio.js'
import { formatearEntero } from '../../../shared/utils/formatters.js'
import { AvisoEtapaCerrada } from '../../periodos/components/AvisoEtapaCerrada.jsx'
import { mapeoParaEnviar, revisarMapeo } from '../mapeo.js'
import { guardarPlantilla, importarCsv, validarCsv } from '../services/importacionesService.js'
import { EditorMapeo } from './EditorMapeo.jsx'
import { ReporteValidacion } from './ReporteValidacion.jsx'

const TAMANO_MAXIMO = 2 * 1024 * 1024
const AUTOMATICO = ''

/** Mapeo editable con todas las columnas del archivo; las que no vienen en el reporte quedan sin campo. */
function mapeoDesde(reporte) {
  return Object.fromEntries(reporte.columnas.map((columna) => [columna, reporte.mapeo[columna] ?? '']))
}

/**
 * Carga del CSV de evaluación de estudiantes: elegir archivo → validar (sin guardar) → ajustar el
 * mapeo de columnas y volver a validar → importar con el mismo mapeo. Todo o nada.
 */
export function CargaCsv({ plantillas, alImportar }) {
  const idArchivo = useId()
  const { periodo } = usePeriodoActivo()
  const recoleccion = useVentanaEtapa(ETAPAS.RECOLECCION_CALIFICACIONES)

  const [archivo, setArchivo] = useState(null)
  const [plantilla, setPlantilla] = useState(AUTOMATICO)
  const [reporte, setReporte] = useState(null)
  const [mapeo, setMapeo] = useState({})
  const [mapeoEditado, setMapeoEditado] = useState(false)
  const [ocupado, setOcupado] = useState(null) // 'validando' | 'importando' | 'plantilla'
  const [error, setError] = useState(null)
  const [resultado, setResultado] = useState(null)
  const [nombrePlantilla, setNombrePlantilla] = useState('')
  const [plantillaGuardada, setPlantillaGuardada] = useState(null)

  const { faltantes, repetidos } = revisarMapeo(mapeo)
  const mapeoCompleto = faltantes.length === 0 && repetidos.length === 0
  const recoleccionCerrada = Boolean(recoleccion && !recoleccion.abierta)
  const puedeImportar =
    reporte?.valido && !mapeoEditado && !recoleccionCerrada && Boolean(periodo) && !ocupado && !resultado

  function elegirArchivo(evento) {
    const elegido = evento.target.files?.[0] ?? null
    setArchivo(elegido)
    setReporte(null)
    setMapeo({})
    setMapeoEditado(false)
    setResultado(null)
    setPlantillaGuardada(null)
    setError(
      elegido && elegido.size > TAMANO_MAXIMO
        ? { message: 'El archivo pesa más de 2 MB. Divide el archivo o quita columnas que no se usan.' }
        : null,
    )
  }

  async function validar(opciones) {
    setOcupado('validando')
    setError(null)
    setResultado(null)
    setPlantillaGuardada(null)
    try {
      const nuevo = await validarCsv(archivo, opciones)
      setReporte(nuevo)
      setMapeo(mapeoDesde(nuevo))
      setMapeoEditado(false)
    } catch (e) {
      setError(e)
    } finally {
      setOcupado(null)
    }
  }

  async function importar() {
    setOcupado('importando')
    setError(null)
    try {
      // Se envía el mismo mapeo que se validó, para importar exactamente lo que se revisó.
      const nuevo = await importarCsv(archivo, { mapeo: mapeoParaEnviar(mapeo) })
      setResultado(nuevo.importacion)
      setReporte(nuevo.reporte)
      alImportar()
    } catch (e) {
      setError(e)
    } finally {
      setOcupado(null)
    }
  }

  async function guardarComoPlantilla(evento) {
    evento.preventDefault()
    setOcupado('plantilla')
    setError(null)
    try {
      const guardada = await guardarPlantilla({
        nombre: nombrePlantilla.trim(),
        columnas: reporte.columnas,
        mapeo: mapeoParaEnviar(mapeo),
      })
      setPlantillaGuardada(guardada.nombre)
      setNombrePlantilla('')
      plantillas.recargar()
    } catch (e) {
      setError(e)
    } finally {
      setOcupado(null)
    }
  }

  function cambiarMapeo(columna, campo) {
    setMapeo((m) => ({ ...m, [columna]: campo }))
    setMapeoEditado(true)
    setPlantillaGuardada(null)
  }

  const listaPlantillas = plantillas.datos ?? []

  return (
    <>
      <Panel
        titulo="1. Archivo de evaluación de estudiantes"
        descripcion="CSV separado por punto y coma o coma, en UTF-8 o como lo guarda Excel. Máximo 2 MB y 5.000 filas."
      >
        <div className="carga__archivo">
          <div className="campo">
            <label htmlFor={idArchivo} className="rotulo">
              Archivo CSV
            </label>
            <input
              id={idArchivo}
              type="file"
              accept=".csv,text/csv"
              className="campo__control carga__selector-archivo"
              onChange={elegirArchivo}
              disabled={Boolean(ocupado)}
              aria-describedby={`${idArchivo}-ayuda`}
            />
            <span id={`${idArchivo}-ayuda`} className="campo__ayuda">
              Una fila por estudiante y docente: correo del docente, los cinco aspectos de 0 a 5 y un comentario
              opcional. Las demás columnas se ignoran.
            </span>
          </div>

          <div className="campo">
            <label htmlFor="plantilla" className="rotulo">
              Mapeo de columnas
            </label>
            <select
              id="plantilla"
              className="campo__control"
              value={plantilla}
              onChange={(e) => setPlantilla(e.target.value)}
              disabled={Boolean(ocupado)}
            >
              <option value={AUTOMATICO}>Detectar automáticamente</option>
              {listaPlantillas.map((p) => (
                <option key={p.idPlantilla} value={p.nombre}>
                  Plantilla: {p.nombre}
                </option>
              ))}
            </select>
            <span className="campo__ayuda">
              Automático reconoce los encabezados habituales o aplica una plantilla con las mismas columnas.
            </span>
          </div>
        </div>

        <div className="carga__acciones">
          <Boton
            icono={FileSearch}
            onClick={() => validar(plantilla ? { plantilla } : {})}
            disabled={!archivo || Boolean(ocupado) || archivo.size > TAMANO_MAXIMO}
          >
            {ocupado === 'validando' && !reporte ? 'Validando…' : reporte ? 'Validar de nuevo' : 'Validar archivo'}
          </Boton>
          <span className="campo__ayuda">Validar no guarda nada.</span>
        </div>

        {!periodo && (
          <Aviso tipo="alerta">No hay un periodo académico activo: puedes validar, pero no importar.</Aviso>
        )}
        <AvisoEtapaCerrada ventana={recoleccion} accion="Importar el CSV" />
        {error && <Aviso tipo="error">{error.message}</Aviso>}
      </Panel>

      {reporte && (
        <Panel
          titulo="2. Columnas del archivo"
          descripcion="Indica qué campo trae cada columna. Las columnas sin campo se ignoran."
          acciones={
            <span className="pildora pildora--info">
              {ETIQUETA_ORIGEN_MAPEO[reporte.origenMapeo] ?? reporte.origenMapeo}
              {reporte.plantilla ? `: ${reporte.plantilla}` : ''}
            </span>
          }
        >
          {(reporte.columnasFaltantes?.length > 0 || reporte.columnasNuevas?.length > 0) && (
            <Aviso tipo="alerta" titulo="El archivo no tiene las mismas columnas que la plantilla">
              {reporte.columnasFaltantes.length > 0 && <>Faltan: {reporte.columnasFaltantes.join(', ')}. </>}
              {reporte.columnasNuevas.length > 0 && <>Nuevas: {reporte.columnasNuevas.join(', ')}.</>}
            </Aviso>
          )}

          <EditorMapeo
            columnas={reporte.columnas}
            mapeo={mapeo}
            onCambiar={cambiarMapeo}
            deshabilitado={Boolean(ocupado) || Boolean(resultado)}
          />

          {mapeoEditado && (
            <div className="carga__acciones">
              <Boton icono={FileSearch} onClick={() => validar({ mapeo: mapeoParaEnviar(mapeo) })} disabled={Boolean(ocupado) || !mapeoCompleto}>
                {ocupado === 'validando' ? 'Validando…' : 'Validar con este mapeo'}
              </Boton>
              <Boton
                variante="secundario"
                onClick={() => {
                  setMapeo(mapeoDesde(reporte))
                  setMapeoEditado(false)
                }}
                disabled={Boolean(ocupado)}
              >
                Deshacer cambios
              </Boton>
              <span className="campo__ayuda">Cambiaste el mapeo: valida de nuevo antes de importar.</span>
            </div>
          )}

          {reporte.valido && !mapeoEditado && (
            <form className="carga__plantilla" onSubmit={guardarComoPlantilla}>
              <div className="campo">
                <label htmlFor="nombre-plantilla" className="rotulo">
                  Guardar este mapeo como plantilla
                </label>
                <input
                  id="nombre-plantilla"
                  className="campo__control"
                  value={nombrePlantilla}
                  maxLength={100}
                  placeholder="Ej.: Encuesta Power BI 2026-2"
                  onChange={(e) => setNombrePlantilla(e.target.value)}
                />
                <span className="campo__ayuda">Si ya tienes una plantilla con ese nombre, se reemplaza.</span>
              </div>
              <Boton type="submit" variante="contorno" icono={Save} disabled={!nombrePlantilla.trim() || Boolean(ocupado)}>
                {ocupado === 'plantilla' ? 'Guardando…' : 'Guardar plantilla'}
              </Boton>
            </form>
          )}
          {plantillaGuardada && <Aviso tipo="exito" titulo={`Plantilla "${plantillaGuardada}" guardada`} />}
        </Panel>
      )}

      {reporte && (
        <Panel titulo="3. Resultado de la validación">
          <ReporteValidacion reporte={reporte} />

          {resultado && <ResumenImportacion importacion={resultado} />}

          {!resultado && (
            <div className="carga__acciones carga__importar">
              <Boton icono={Upload} onClick={importar} disabled={!puedeImportar}>
                {ocupado === 'importando' ? 'Importando…' : `Importar en ${periodo?.nombre ?? 'el periodo activo'}`}
              </Boton>
              <span className="campo__ayuda">
                Crea o reemplaza la nota de estudiantes de cada docente y recalcula su promedio. Volver a cargar el
                mismo archivo no duplica notas ni comentarios.
              </span>
            </div>
          )}
        </Panel>
      )}
    </>
  )
}

function ResumenImportacion({ importacion }) {
  if (importacion.resultado !== 'EXITOSA') {
    return (
      <Aviso tipo="error" titulo={`La carga #${importacion.idImportacion} fue rechazada`}>
        El archivo tiene errores y no se guardó ninguna nota ni comentario. Corrígelo y vuelve a validarlo.
      </Aviso>
    )
  }
  const datos = [
    ['Docentes afectados', importacion.docentesAfectados],
    ['Notas nuevas', importacion.notasNuevas],
    ['Notas corregidas', importacion.notasCorregidas],
    ['Notas sin cambios', importacion.notasSinCambios],
    ['Comentarios nuevos', importacion.comentariosNuevos],
    ['Comentarios repetidos', importacion.comentariosRepetidos],
  ]
  return (
    <Aviso tipo="exito" titulo={`Carga #${importacion.idImportacion} importada en ${importacion.nombrePeriodo}`}>
      <dl className="carga__resumen">
        {datos.map(([clave, valor]) => (
          <div key={clave}>
            <dt>{clave}</dt>
            <dd>{formatearEntero(valor)}</dd>
          </div>
        ))}
      </dl>
      Las notas corregidas quedaron en el historial de correcciones de cada docente.
    </Aviso>
  )
}
