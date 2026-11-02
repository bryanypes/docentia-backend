import { Download, RefreshCw, Upload, Users } from 'lucide-react'
import { useCallback, useRef, useState } from 'react'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ETIQUETA_ROL } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { useSesion } from '../../auth/sesionContext.js'
import { descargarPlantilla, importarMatricula, obtenerCobertura } from '../services/matriculaService.js'
import '../../actas/components/PanelActa.css'
import '../../notificaciones/components/Notificaciones.css'

function porcentaje(valor) {
  return `${Number(valor).toLocaleString('es-CO')} %`
}

function Importacion({ alImportar }) {
  const [archivo, setArchivo] = useState(null)
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

  return (
    <Panel titulo="Importar matrícula" descripcion="CSV del periodo activo. Si una fila tiene error, no se guarda nada." icono={Upload}>
      <div className="acta__formulario">
        <Campo id="archivoMatricula" etiqueta="Archivo CSV" ayuda="Coma o punto y coma, UTF-8, máximo 5 MB.">
          <input
            id="archivoMatricula"
            type="file"
            accept=".csv,text/csv"
            className="campo__control"
            onChange={(e) => setArchivo(e.target.files?.[0] ?? null)}
          />
        </Campo>
        <div className="notificaciones__barra">
          <Boton icono={Upload} disabled={!archivo || ocupado} onClick={importar}>
            {ocupado ? 'Importando…' : 'Importar'}
          </Boton>
          <Boton variante="contorno" icono={Download} onClick={plantilla}>
            Descargar plantilla
          </Boton>
        </div>
      </div>
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

export function TablaCobertura({ filas, primeraColumna }) {
  return (
    <div className="acciones__tabla">
      <table className="tabla">
        <thead>
          <tr>
            <th scope="col">{primeraColumna}</th>
            <th scope="col">Matriculados</th>
            <th scope="col">Respondieron</th>
            <th scope="col">Cobertura</th>
          </tr>
        </thead>
        <tbody>
          {filas.map((f) => (
            <tr key={`${f.idDocente ?? ''}-${f.nombre}`}>
              <td>{f.nombre}</td>
              <td>{f.matriculados}</td>
              <td>{f.respondieron}</td>
              <td>{porcentaje(f.porcentaje)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default function Matricula() {
  const { usuario } = useSesion()
  const [pagina, setPagina] = useState(0)
  const cargar = useCallback(() => obtenerCobertura(pagina), [pagina])
  const cobertura = useFetch(cargar, [cargar])
  const datos = cobertura.datos

  return (
    <>
      <Encabezado
        miga={`${ETIQUETA_ROL[usuario.rol]} · Matrícula y cobertura`}
        titulo="Matrícula y cobertura"
        subtitulo="Quién debe evaluar a cada profesor y cuántos ya respondieron. Nunca se ve quién respondió qué."
      />
      <div className="pagina">
        <Importacion alImportar={cobertura.recargar} />
        {cobertura.cargando && !datos && <Cargando texto="Cargando la cobertura" bloques={1} />}
        {cobertura.error && (
          <EstadoVista tipo="error" titulo="No fue posible cargar la cobertura" mensaje={cobertura.error.message}>
            <Boton icono={RefreshCw} onClick={cobertura.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}
        {datos && (
          <Panel
            titulo={`Cobertura · ${datos.periodo.nombre}`}
            descripcion={`${datos.total.respondieron} de ${datos.total.matriculados} (${porcentaje(datos.total.porcentaje)})`}
            icono={Users}
          >
            {datos.docentes.contenido.length === 0 ? (
              <p className="campo__ayuda">Aún no hay matrícula en este periodo.</p>
            ) : (
              <>
                <TablaCobertura filas={datos.docentes.contenido} primeraColumna="Docente" />
                {datos.docentes.totalPaginas > 1 && (
                  <div className="notificaciones__paginas">
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
                <h3 className="acta__subtitulo">Por programa</h3>
                <TablaCobertura filas={datos.programas} primeraColumna="Programa" />
              </>
            )}
          </Panel>
        )}
      </div>
    </>
  )
}
