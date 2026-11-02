import { ClipboardCheck, RefreshCw } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { obtenerListaHeteroevaluacion } from '../services/formulariosService.js'
import '../components/FormularioInstrumento.css'

const ESTADO = {
  SIN_INICIAR: { etiqueta: 'Sin iniciar', clase: 'neutro' },
  BORRADOR: { etiqueta: 'En borrador', clase: 'info' },
  ENVIADA: { etiqueta: 'Enviada', clase: 'exito' },
}

export default function EvaluacionProfesores() {
  const { datos, error, cargando, recargar } = useFetch(obtenerListaHeteroevaluacion)

  return (
    <>
      <Encabezado
        miga="Estudiante · Evaluación a profesor"
        titulo="Evaluación a profesor"
        subtitulo={
          datos
            ? `Periodo ${datos.periodo.nombre}. ${datos.pendientes} pendientes. Un formulario por grupo, no por persona.`
            : 'Califica a cada profesor por la clase en la que estás matriculado. Un formulario por grupo, no por persona.'
        }
      />
      <div className="pagina">
        {cargando && !datos && <Cargando texto="Cargando tus clases" bloques={1} />}
        {error && (
          <EstadoVista tipo="error" titulo="No fue posible cargar tus profesores" mensaje={error.message}>
            <Boton icono={RefreshCw} onClick={recargar}>
              Intentar de nuevo
            </Boton>
          </EstadoVista>
        )}
        {!cargando && !error && !datos && (
          <EstadoVista titulo="No hay un periodo activo" mensaje="La vicerrectoría debe activar uno." />
        )}
        {datos?.clases.length === 0 && (
          <EstadoVista
            titulo="Sin clases en este periodo"
            mensaje="Cuando te matriculen en un curso verás aquí a cada profesor para evaluarlo."
          />
        )}
        {datos?.clases.length > 0 && (
          <Panel titulo="Tus profesores por clase" icono={ClipboardCheck}>
            <div className="coevaluaciones__tabla">
              <table className="tabla">
                <thead>
                  <tr>
                    <th scope="col">Profesor</th>
                    <th scope="col">Curso</th>
                    <th scope="col">Grupo</th>
                    <th scope="col">Programa</th>
                    <th scope="col">Estado</th>
                    <th scope="col">Avance</th>
                  </tr>
                </thead>
                <tbody>
                  {datos.clases.map((clase) => (
                    <tr key={`${clase.idClase}-${clase.idDocente}`}>
                      <td>
                        <Link to={`/evaluacion-profesores/${clase.idClase}/${clase.idDocente}`}>
                          {clase.nombreDocente}
                        </Link>
                      </td>
                      <td>{clase.nombreCurso}</td>
                      <td>{clase.grupo}</td>
                      <td>{clase.nombrePrograma}</td>
                      <td>
                        <span className={`pildora pildora--${ESTADO[clase.estado].clase}`}>
                          {ESTADO[clase.estado].etiqueta}
                        </span>
                      </td>
                      <td>
                        {clase.respondidos} de {datos.itemsHeteroevaluacion}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Panel>
        )}
      </div>
    </>
  )
}
