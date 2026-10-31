import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { ASPECTOS } from '../../../shared/constants/dominio.js'
import { formatearEntero, formatearNota } from '../../../shared/utils/formatters.js'

const NOMBRE_DELIMITADOR = { ';': 'punto y coma (;)', ',': 'coma (,)' }
// El backend devuelve como máximo 500 errores; `filasConError` trae el total.
const MAX_ERRORES = 500

/** Resumen, advertencias, errores por fila y notas por docente del reporte de /importaciones/csv/validar. */
export function ReporteValidacion({ reporte }) {
  const { errores, docentes, advertencias } = reporte

  return (
    <div className="reporte">
      <dl className="reporte__resumen">
        <Dato clave="Archivo" valor={reporte.nombreArchivo} />
        <Dato clave="Codificación" valor={reporte.codificacion ?? '—'} />
        <Dato clave="Separador" valor={NOMBRE_DELIMITADOR[reporte.delimitador] ?? reporte.delimitador ?? '—'} />
        <Dato clave="Filas procesadas" valor={formatearEntero(reporte.filasProcesadas)} />
        <Dato clave="Filas con error" valor={formatearEntero(reporte.filasConError)} />
        <Dato clave="Docentes" valor={formatearEntero(docentes.length)} />
      </dl>

      {reporte.valido ? (
        <Aviso tipo="exito" titulo="El archivo es válido">
          Todas las filas cumplen las reglas. Revisa las notas por docente antes de importar.
        </Aviso>
      ) : (
        <Aviso tipo="error" titulo="El archivo tiene errores">
          Con un solo error no se guarda nada. Corrige el archivo o el mapeo de columnas y vuelve a validar.
        </Aviso>
      )}

      {advertencias.length > 0 && (
        <Aviso tipo="alerta" titulo="Advertencias">
          <ul className="reporte__advertencias">
            {advertencias.map((a) => (
              <li key={a}>{a}</li>
            ))}
          </ul>
        </Aviso>
      )}

      {errores.length > 0 && (
        <section aria-labelledby="reporte-errores">
          <h3 id="reporte-errores" className="reporte__subtitulo">
            Errores ({formatearEntero(errores.length)})
          </h3>
          {errores.length >= MAX_ERRORES && (
            <p className="reporte__ayuda">
              Se muestran los primeros {formatearEntero(MAX_ERRORES)}; en total hay{' '}
              {formatearEntero(reporte.filasConError)} filas con error.
            </p>
          )}
          <div className="tabla-contenedor">
            <table className="tabla">
              <thead>
                <tr>
                  <th scope="col" className="tabla__numero">
                    Fila
                  </th>
                  <th scope="col">Columna</th>
                  <th scope="col">Valor</th>
                  <th scope="col">Motivo</th>
                </tr>
              </thead>
              <tbody>
                {errores.map((e, i) => (
                  <tr key={`${e.fila}-${e.columna}-${i}`}>
                    <td className="tabla__numero">{e.fila ?? 'Archivo'}</td>
                    <td>{e.columna ?? (e.fila === null ? '—' : 'Fila completa')}</td>
                    <td>{e.valor ? <code className="reporte__valor">{e.valor}</code> : '—'}</td>
                    <td>{e.motivo}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {docentes.length > 0 && (
        <section aria-labelledby="reporte-docentes">
          <h3 id="reporte-docentes" className="reporte__subtitulo">
            Notas de estudiantes por docente
          </h3>
          <p className="reporte__ayuda">
            Si un docente tiene varias filas, cada aspecto es el promedio de esas filas. La nota de la fuente es el
            promedio de los cinco aspectos.
          </p>
          <div className="tabla-contenedor">
            <table className="tabla reporte__docentes">
              <thead>
                <tr>
                  <th scope="col">Docente</th>
                  <th scope="col" className="tabla__numero">
                    Filas
                  </th>
                  {ASPECTOS.map((a) => (
                    <th key={a.campo} scope="col" className="tabla__numero" title={a.nombre}>
                      {a.nombre}
                    </th>
                  ))}
                  <th scope="col" className="tabla__numero">
                    Nota
                  </th>
                </tr>
              </thead>
              <tbody>
                {docentes.map((d) => (
                  <tr key={d.idDocente}>
                    <td>
                      <span className="tabla__fuerte">{d.nombreCompleto}</span>
                      <span className="tabla__tenue">
                        {d.correo} · {d.comentarios} {d.comentarios === 1 ? 'comentario' : 'comentarios'}
                      </span>
                    </td>
                    <td className="tabla__numero">
                      {d.filasConNotas}/{d.filas}
                    </td>
                    {ASPECTOS.map((a) => (
                      <td key={a.campo} className="tabla__numero">
                        {formatearNota(d[a.campo])}
                      </td>
                    ))}
                    <td className="tabla__numero tabla__fuerte">
                      {d.notaFuente === null ? 'Solo comentarios' : formatearNota(d.notaFuente)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  )
}

function Dato({ clave, valor }) {
  return (
    <div className="reporte__dato">
      <dt className="rotulo">{clave}</dt>
      <dd>{valor}</dd>
    </div>
  )
}
