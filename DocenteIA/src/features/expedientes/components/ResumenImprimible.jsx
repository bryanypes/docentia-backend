import { useState } from 'react'
import { createPortal } from 'react-dom'
import robotAprobada from '../../../assets/ilustraciones/robot-aprobada.svg'
import {
  etiquetaFuente,
  formatearFecha,
  formatearNota,
  formatearNotaFuente,
  formatearPeso,
} from '../../../shared/utils/formatters.js'
import { ESTADOS_PROCESO } from '../../proceso/estados.js'
import './ResumenImprimible.css'

const LOGO = `${import.meta.env.BASE_URL}brand/logo-uniautonoma.png`

/**
 * Resumen del docente para imprimir o guardar en PDF. Se monta directo en <body>: dentro del
 * contenedor con scroll de la app el navegador lo recorta. En pantalla no se ve.
 */
export function ResumenImprimible({ docente, periodo, promedio, notasPorFuente, compromisos, proceso }) {
  const completo = promedio?.estado === 'COMPLETO'
  const categoria = completo ? promedio.categoria : null
  const [hoy] = useState(() =>
    new Date().toLocaleDateString('es-CO', { day: 'numeric', month: 'long', year: 'numeric' }),
  )

  return createPortal(
    <article className="resumen-impreso" aria-label="Resumen imprimible del docente">
      <header className="resumen-impreso__cabecera">
        <img className="resumen-impreso__logo" src={LOGO} alt="Corporación Universitaria Autónoma del Cauca" />
        <div className="resumen-impreso__marca">
          <strong>DOCENTIA</strong>
          <span>Evaluación docente · Resolución 074</span>
        </div>
        <img className="resumen-impreso__robot" src={robotAprobada} alt="" />
      </header>

      <h1 className="resumen-impreso__titulo">Resumen de evaluación docente</h1>
      <p className="resumen-impreso__sub">
        Periodo {periodo?.nombre ?? 'sin periodo activo'} · generado el {hoy}
      </p>

      <section className="resumen-impreso__principal">
        <div className="resumen-impreso__persona">
          <span className="resumen-impreso__rotulo">Docente</span>
          <strong>{docente.nombreCompleto}</strong>
          <span>{docente.correo}</span>
          <span>
            {docente.nombrePrograma ?? `Coordinación · ${docente.nombreFacultad}`}
            {docente.nombrePrograma && docente.nombreFacultad ? ` · ${docente.nombreFacultad}` : ''}
          </span>
          <span className="resumen-impreso__rotulo resumen-impreso__rotulo--espacio">Estado del proceso</span>
          <span>{ESTADOS_PROCESO[proceso?.estado]?.etiqueta ?? 'No disponible'}</span>
        </div>
        <div className={`resumen-impreso__nota${completo ? '' : ' resumen-impreso__nota--pendiente'}`}>
          <span className="resumen-impreso__rotulo">Nota final</span>
          <strong>{completo ? formatearNota(promedio.valor) : 'Pendiente'}</strong>
          <span>{categoria ? categoria.etiqueta : 'Faltan notas para calcular el promedio'}</span>
        </div>
      </section>

      {categoria?.accionInstitucional && (
        <p className="resumen-impreso__accion">
          <strong>Acción institucional:</strong> {categoria.accionInstitucional}
        </p>
      )}

      <h2>Notas por evaluador</h2>
      <table className="resumen-impreso__tabla">
        <thead>
          <tr>
            <th scope="col">Evaluador</th>
            <th scope="col">Peso</th>
            <th scope="col">Nota</th>
            <th scope="col" className="resumen-impreso__columna-barra">
              <span className="solo-lectores">Escala de 0 a 5</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {notasPorFuente.map((fuente) => (
            <tr key={fuente.fuente}>
              <th scope="row">{etiquetaFuente(fuente.fuente, docente.esCoordinador)}</th>
              <td>{formatearPeso(fuente.peso)}</td>
              <td className="resumen-impreso__cifra">
                {formatearNotaFuente(fuente.nota)}
                {fuente.nota === null && <small> sin registrar</small>}
              </td>
              <td>
                <span className="resumen-impreso__barra">
                  <span style={{ width: `${(Number(fuente.nota ?? 0) / 5) * 100}%` }} />
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <h2>Compromisos de mejora vigentes</h2>
      {compromisos.length ? (
        <ol className="resumen-impreso__compromisos">
          {compromisos.map((c) => (
            <li key={c.idCompromiso}>
              {c.descripcion}
              <span>Vence el {formatearFecha(c.fechaLimite)}</span>
            </li>
          ))}
        </ol>
      ) : (
        <p className="resumen-impreso__vacio">No hay compromisos de mejora vigentes.</p>
      )}

      <footer className="resumen-impreso__pie">
        Corporación Universitaria Autónoma del Cauca · DOCENTIA. Documento informativo generado desde la
        plataforma; las notas por evaluador se reportan dentro del promedio ponderado.
      </footer>
    </article>,
    document.body,
  )
}
