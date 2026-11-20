import { CircleAlert, CircleCheck, CircleX } from 'lucide-react'
import { useCallback } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Cargando } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { useTituloDocumento } from '../../../shared/hooks/useTituloDocumento.js'
import { formatearFechaHora } from '../../../shared/utils/formatters.js'
import { FormularioCodigo } from '../components/FormularioCodigo.jsx'
import { verificarActa } from '../services/actasService.js'
import './VerificarActa.css'

/**
 * Página pública: confirma que un código corresponde a un acta firmada. No muestra notas. Sin
 * código en la dirección, solo pide escribirlo.
 */
export default function VerificarActa() {
  useTituloDocumento('Verificación de actas')
  const { codigo } = useParams()
  const navegar = useNavigate()

  return (
    <main className="verificacion">
      <div className="verificacion__hoja">
        <header className="verificacion__membrete">
          <img
            src={`${import.meta.env.BASE_URL}brand/logo-uniautonoma.png`}
            alt="Corporación Universitaria Autónoma del Cauca"
          />
          <div>
            <p className="verificacion__institucion">Corporación Universitaria Autónoma del Cauca</p>
            <p className="verificacion__dependencia">Vicerrectoría Académica · Evaluación docente</p>
          </div>
        </header>
        <h1 className="verificacion__titulo">Verificación de actas</h1>
        <p className="verificacion__ayuda">
          Escriba el código que aparece en la parte superior del acta para comprobar que el documento es auténtico y
          está vigente.
        </p>
        <FormularioCodigo
          key={codigo ?? ''}
          inicial={codigo ?? ''}
          alBuscar={(nuevo) => navegar(`/verificar-acta/${encodeURIComponent(nuevo)}`)}
        />
        {codigo && <Resultado codigo={codigo} />}
        <p className="verificacion__pie">
          <Link to="/">Ir a DOCENTIA</Link>
        </p>
      </div>
    </main>
  )
}

function Resultado({ codigo }) {
  const cargar = useCallback(() => verificarActa(codigo), [codigo])
  const { datos, error, cargando } = useFetch(cargar, [cargar])

  if (cargando) return <Cargando texto="Verificando" bloques={1} />
  if (error) {
    const noExiste = error.status === 404
    return (
      <section className="verificacion__resultado verificacion__resultado--error" role="alert">
        <CircleX size={28} aria-hidden="true" />
        <div>
          <h2>{noExiste ? 'Código no encontrado' : 'No se pudo verificar'}</h2>
          <p>
            {noExiste
              ? `El código ${codigo.toUpperCase()} no corresponde a ningún acta. Revise que esté escrito tal como aparece en el documento.`
              : error.message}
          </p>
        </div>
      </section>
    )
  }
  if (!datos) return null
  const vigente = datos.estado === 'FIRMADA'
  const Icono = vigente ? CircleCheck : CircleAlert
  return (
    <section
      className={`verificacion__resultado verificacion__resultado--${vigente ? 'valida' : 'anulada'}`}
      role="status"
    >
      <Icono size={28} aria-hidden="true" />
      <div>
        <h2>{vigente ? 'Acta válida' : 'Acta reemplazada'}</h2>
        <p>
          {vigente
            ? 'El documento es auténtico y está vigente.'
            : 'Este documento fue reemplazado por una corrección; solicite el acta vigente.'}
        </p>
        <dl className="verificacion__datos">
          <dt>Docente</dt>
          <dd>{datos.nombreDocente}</dd>
          <dt>Periodo académico</dt>
          <dd>{datos.periodo}</dd>
          {datos.fechaFirma && (
            <>
              <dt>Firmada el</dt>
              <dd>{formatearFechaHora(datos.fechaFirma)}</dd>
            </>
          )}
          <dt>Código</dt>
          <dd>{codigo.toUpperCase()}</dd>
          <dt>Huella SHA-256</dt>
          <dd className="verificacion__huella">{datos.hashContenido}</dd>
        </dl>
      </div>
    </section>
  )
}
