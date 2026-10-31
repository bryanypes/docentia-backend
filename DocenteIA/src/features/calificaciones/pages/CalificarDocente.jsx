import { ChevronLeft, RefreshCw, RotateCcw, Save } from 'lucide-react'
import { useCallback, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import robotAprobada from '../../../assets/ilustraciones/robot-aprobada.svg'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import {
  usePeriodoActivo,
  useVentanaEtapa,
} from '../../../shared/components/layout/AppLayout/periodoContext.js'
import { BarraNota } from '../../../shared/components/ui/BarraNota/BarraNota.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { ASPECTOS, ETAPAS, ETIQUETA_FUENTE, FUENTE_DEL_ROL } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearNota, iniciales } from '../../../shared/utils/formatters.js'
import { aNumero, validarNotaAspecto } from '../../../shared/utils/validators.js'
import { useSesion } from '../../auth/sesionContext.js'
import { PanelFuentes } from '../../expedientes/components/PanelesExpediente.jsx'
import { obtenerExpedienteDocente } from '../../expedientes/services/expedientesService.js'
import { AvisoEtapaCerrada } from '../../periodos/components/AvisoEtapaCerrada.jsx'
import { guardarCalificacion, obtenerCalificacion } from '../services/calificacionesService.js'
import './CalificarDocente.css'

const MAX_OBSERVACION = 1000
const MAX_MOTIVO = 500

/** Una calificación ya guardada cuyas notas cambian es una corrección (D2); la observación no cuenta. */
function notasCambiaron(guardada, notas) {
  if (!guardada) return false
  return ASPECTOS.some(({ campo }) => aNumero(notas[campo]) !== Number(guardada[campo]))
}

function formularioDesde(calificacion) {
  const notas = Object.fromEntries(
    // Con coma decimal, como pide la instrucción ("de 1,0 a 5,0"); aNumero acepta coma o punto.
    ASPECTOS.map(({ campo }) => [campo, calificacion ? String(calificacion[campo]).replace('.', ',') : '']),
  )
  return { notas, observacion: calificacion?.observacion ?? '', motivoCorreccion: '' }
}

export default function CalificarDocente() {
  const { idDocente } = useParams()
  const { usuario } = useSesion()
  const { periodo } = usePeriodoActivo()
  const fuente = FUENTE_DEL_ROL[usuario.rol]
  const recoleccion = useVentanaEtapa(ETAPAS.RECOLECCION_CALIFICACIONES)

  const cargar = useCallback(
    () => Promise.all([obtenerExpedienteDocente(idDocente), obtenerCalificacion(idDocente, fuente)]),
    [idDocente, fuente],
  )
  const datos = useFetch(cargar, [cargar])
  // false, 'registro' o 'correccion': el formulario se remonta al guardar y el aviso debe seguir visible.
  const [exito, setExito] = useState(false)

  const encabezado = (
    <Encabezado
      miga="Docentes · Calificar"
      titulo={`Evaluación de ${ETIQUETA_FUENTE[fuente].toLowerCase()}`}
      subtitulo="Registra tu calificación por aspecto. El docente nunca verá esta nota por separado: solo el promedio ponderado del periodo."
    />
  )

  if (datos.cargando && !datos.datos) {
    return (
      <>
        {encabezado}
        <div className="pagina">
          <Cargando texto="Cargando calificación" bloques={2} />
        </div>
      </>
    )
  }

  if (datos.error) {
    const noEncontrado = datos.error.status === 404
    return (
      <>
        {encabezado}
        <div className="pagina">
          <EstadoVista
            tipo={noEncontrado ? 'vacio' : 'error'}
            titulo={noEncontrado ? 'Docente no encontrado' : 'No fue posible cargar la calificación'}
            mensaje={noEncontrado ? 'El docente no existe o no está dentro de tu alcance.' : datos.error.message}
          >
            {noEncontrado ? (
              <Link to="/docentes">Volver al listado</Link>
            ) : (
              <Boton icono={RefreshCw} onClick={datos.recargar}>Intentar de nuevo</Boton>
            )}
          </EstadoVista>
        </div>
      </>
    )
  }

  const [expediente, calificacion] = datos.datos

  return (
    <>
      {encabezado}
      <div className="pagina">
        {/* La key reinicia el formulario cuando cambia la calificación guardada en el servidor. */}
        <FormularioCalificacion
          key={calificacion?.fechaActualizacion ?? 'nueva'}
          expediente={expediente}
          guardada={calificacion}
          fuente={fuente}
          periodo={periodo}
          recoleccion={recoleccion}
          exito={exito}
          setExito={setExito}
          alGuardar={datos.recargar}
        />
      </div>
    </>
  )
}

function FormularioCalificacion({
  expediente,
  guardada,
  fuente,
  periodo,
  recoleccion,
  exito,
  setExito,
  alGuardar,
}) {
  const { docente } = expediente
  const [formulario, setFormulario] = useState(() => formularioDesde(guardada))
  const [tocados, setTocados] = useState({})
  const [enviando, setEnviando] = useState(false)
  const [errorEnvio, setErrorEnvio] = useState(null)

  const erroresLocales = Object.fromEntries(
    ASPECTOS.map(({ campo }) => [campo, validarNotaAspecto(formulario.notas[campo])]),
  )
  const erroresServidor = errorEnvio?.errores ?? {}
  const esCorreccion = notasCambiaron(guardada, formulario.notas)
  const recoleccionCerrada = Boolean(recoleccion && !recoleccion.abierta)
  const formularioValido =
    ASPECTOS.every(({ campo }) => !erroresLocales[campo]) &&
    formulario.observacion.length <= MAX_OBSERVACION &&
    formulario.motivoCorreccion.length <= MAX_MOTIVO
  const promedioPropio = formularioValido
    ? ASPECTOS.reduce((suma, { campo }) => suma + aNumero(formulario.notas[campo]), 0) / ASPECTOS.length
    : null

  function cambiarNota(campo, valor) {
    setFormulario((f) => ({ ...f, notas: { ...f.notas, [campo]: valor } }))
    setExito(false)
  }

  function restablecer() {
    setFormulario(formularioDesde(guardada))
    setTocados({})
    setErrorEnvio(null)
    setExito(false)
  }

  async function guardar(evento) {
    evento.preventDefault()
    setTocados(Object.fromEntries(ASPECTOS.map(({ campo }) => [campo, true])))
    if (!formularioValido) return

    setEnviando(true)
    setErrorEnvio(null)
    try {
      const cuerpo = Object.fromEntries(ASPECTOS.map(({ campo }) => [campo, aNumero(formulario.notas[campo])]))
      cuerpo.observacion = formulario.observacion.trim() || null
      cuerpo.motivoCorreccion = esCorreccion ? formulario.motivoCorreccion.trim() || null : null
      await guardarCalificacion(docente.idDocente, fuente, cuerpo)
      setExito(esCorreccion ? 'correccion' : 'registro')
      alGuardar()
    } catch (error) {
      setErrorEnvio(error)
    } finally {
      // Si se guardan las mismas notas el backend no cambia fechaActualizacion y el formulario no se remonta.
      setEnviando(false)
    }
  }

  return (
    <>
      <Link to={`/docentes/${docente.idDocente}`} className="enlace-volver">
        <ChevronLeft size={16} aria-hidden="true" />
        Volver al expediente
      </Link>

      <form className="calificar" onSubmit={guardar} noValidate>
        <Panel>
          <div className="calificar__docente">
            <span className="calificar__avatar" aria-hidden="true">
              {iniciales(docente.nombreCompleto)}
            </span>
            <div>
              <h2 className="calificar__nombre">{docente.nombreCompleto}</h2>
              <p className="calificar__meta">
                {docente.nombrePrograma} · periodo {periodo?.nombre ?? 'sin periodo activo'}
              </p>
            </div>
          </div>

          <p className="calificar__instruccion">
            Califica cada aspecto de 1,0 a 5,0. Los decimales se admiten con un dígito.
          </p>

          <div className="calificar__aspectos">
            {ASPECTOS.map(({ campo, nombre, guia }) => {
              const mensaje = erroresServidor[campo] ?? (tocados[campo] ? erroresLocales[campo] : null)
              const valor = formulario.notas[campo]
              return (
                <div key={campo} className="calificar__aspecto">
                  <div className="calificar__aspecto-fila">
                    <div className="calificar__aspecto-textos">
                      <label htmlFor={campo}>{nombre}</label>
                      <span id={`${campo}-guia`}>{guia}</span>
                    </div>
                    {/* Texto y no number: number descarta la coma ("4,5") según el navegador y la rueda
                        del mouse cambia la nota sin querer. validarNotaAspecto acepta coma o punto. */}
                    <input
                      id={campo}
                      type="text"
                      inputMode="decimal"
                      autoComplete="off"
                      maxLength={3}
                      value={valor}
                      onChange={(e) => cambiarNota(campo, e.target.value)}
                      onBlur={() => setTocados((t) => ({ ...t, [campo]: true }))}
                      aria-invalid={Boolean(mensaje)}
                      aria-describedby={mensaje ? `${campo}-guia ${campo}-error` : `${campo}-guia`}
                      className="calificar__nota"
                    />
                  </div>
                  <BarraNota valor={erroresLocales[campo] ? null : aNumero(valor)} />
                  {mensaje && (
                    <span id={`${campo}-error`} className="calificar__error-campo">
                      {mensaje}
                    </span>
                  )}
                </div>
              )
            })}
          </div>

          <div className="calificar__observacion">
            <label htmlFor="observacion" className="rotulo">
              Observación para la reunión (opcional)
            </label>
            <textarea
              id="observacion"
              value={formulario.observacion}
              maxLength={MAX_OBSERVACION}
              onChange={(e) => {
                setFormulario((f) => ({ ...f, observacion: e.target.value }))
                setExito(false)
              }}
              placeholder="Queda en el expediente con tu calificación."
              aria-describedby="observacion-contador"
            />
            <span id="observacion-contador" className="calificar__contador">
              {formulario.observacion.length} / {MAX_OBSERVACION}
            </span>
          </div>

          {esCorreccion && (
            <div className="calificar__observacion calificar__correccion">
              <label htmlFor="motivoCorreccion" className="rotulo">
                Motivo de la corrección (opcional)
              </label>
              <span id="motivoCorreccion-ayuda" className="campo__ayuda">
                Cambiaste notas que ya estaban guardadas. La nota anterior, la nueva y el motivo quedan en
                el historial de correcciones del docente.
              </span>
              <textarea
                id="motivoCorreccion"
                value={formulario.motivoCorreccion}
                maxLength={MAX_MOTIVO}
                onChange={(e) => {
                  setFormulario((f) => ({ ...f, motivoCorreccion: e.target.value }))
                  setExito(false)
                }}
                placeholder="Ej.: error de digitación en la visita de aula."
                aria-invalid={Boolean(erroresServidor.motivoCorreccion)}
                aria-describedby="motivoCorreccion-ayuda motivoCorreccion-contador"
              />
              {erroresServidor.motivoCorreccion && (
                <span className="calificar__error-campo">{erroresServidor.motivoCorreccion}</span>
              )}
              <span id="motivoCorreccion-contador" className="calificar__contador">
                {formulario.motivoCorreccion.length} / {MAX_MOTIVO}
              </span>
            </div>
          )}
        </Panel>

        <div className="calificar__lateral">
          <section className="calificar__mi-nota" aria-live="polite">
            <div className="calificar__mi-nota-rotulo">Tu calificación</div>
            <div className="calificar__mi-nota-valor">
              <strong>{formatearNota(promedioPropio)}</strong>
              <span>/ 5,0</span>
            </div>
            <p>Promedio simple de los cinco aspectos que calificaste.</p>
          </section>

          <PanelFuentes
            notasPorFuente={expediente.notasPorFuente}
            promedio={expediente.promedio}
            descripcion="Valores guardados en el periodo activo."
          >
            <p className="fuentes__nota-pie">
              Esta es la única nota que verá el docente. Las calificaciones de coordinación y
              vicerrectoría no se publican por separado.
            </p>
          </PanelFuentes>

          <Panel>
            {exito && (
              <div className="calificar__exito" role="status">
                <img src={robotAprobada} alt="" aria-hidden="true" />
                <div>
                  <strong>{exito === 'correccion' ? 'Corrección registrada' : 'Calificación registrada'}</strong>
                  <span>
                    {exito === 'correccion'
                      ? 'El promedio se recalculó y el cambio quedó en el historial de correcciones.'
                      : 'Queda en el expediente con tu nombre y la fecha.'}{' '}
                    Puedes modificarla mientras la recolección de calificaciones esté abierta.
                  </span>
                </div>
              </div>
            )}
            <AvisoEtapaCerrada ventana={recoleccion} accion="Calificar" />
            {errorEnvio && (
              <div className="calificar__error" role="alert">
                {errorEnvio.message}
              </div>
            )}
            {guardada && !exito && (
              <p className="calificar__aviso">
                {esCorreccion
                  ? 'Cambiaste notas ya guardadas: al guardar queda como corrección en el historial.'
                  : 'Ya registraste una calificación: al guardar se reemplaza.'}
              </p>
            )}
            <div className="calificar__botones">
              <Boton type="submit" icono={Save} disabled={enviando || !periodo || recoleccionCerrada}>
                {enviando ? 'Guardando…' : guardada ? 'Actualizar calificación' : 'Guardar calificación'}
              </Boton>
              <Boton variante="secundario" icono={RotateCcw} onClick={restablecer} disabled={enviando}>
                Restablecer
              </Boton>
            </div>
            {!periodo && (
              <p className="calificar__aviso">No hay un periodo académico activo; no es posible calificar.</p>
            )}
          </Panel>
        </div>
      </form>
    </>
  )
}
