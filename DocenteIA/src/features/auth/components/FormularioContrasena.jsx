import { Check, Circle, Eye, EyeOff } from 'lucide-react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { ResumenErrores } from '../../../shared/components/ui/ResumenErrores/ResumenErrores.jsx'
import '../pages/CambiarContrasena.css'

/** Campos, requisitos y botones del cambio de contraseña; el título lo pone quien lo usa. */
export function FormularioContrasena({ cambio, obligatorio = false, encabezado, acciones }) {
  const { formulario, ver, setVer, enviando, resumen, errorDe, listaErrores, errorGeneral, requisitos } = cambio
  const { contrasenaActual, contrasenaNueva, confirmacion } = formulario
  const describir = (campo, ...extra) => [...extra, errorDe(campo) && `${campo}-error`].filter(Boolean).join(' ') || undefined
  const tipo = ver ? 'text' : 'password'

  return (
    <form className="cambio-contrasena__formulario" onSubmit={cambio.guardar} noValidate>
      {encabezado}

      <ResumenErrores ref={resumen} errores={listaErrores} />
      {errorGeneral && (
        <div className="cambio-contrasena__error" role="alert">
          {errorGeneral}
        </div>
      )}

      <Campo id="contrasenaActual" etiqueta={obligatorio ? 'Contraseña temporal' : 'Contraseña actual'} error={errorDe('contrasenaActual')}>
        <input
          id="contrasenaActual"
          type={tipo}
          autoComplete="current-password"
          value={contrasenaActual}
          onChange={(e) => cambio.cambiar('contrasenaActual', e.target.value)}
          aria-invalid={Boolean(errorDe('contrasenaActual'))}
          aria-describedby={describir('contrasenaActual')}
        />
      </Campo>

      <Campo id="contrasenaNueva" etiqueta="Nueva contraseña" error={errorDe('contrasenaNueva')}>
        <input
          id="contrasenaNueva"
          type={tipo}
          autoComplete="new-password"
          maxLength={72}
          value={contrasenaNueva}
          onChange={(e) => cambio.cambiar('contrasenaNueva', e.target.value)}
          aria-invalid={Boolean(errorDe('contrasenaNueva'))}
          aria-describedby={describir('contrasenaNueva', 'requisitos')}
        />
      </Campo>

      <Campo id="confirmacion" etiqueta="Repite la nueva contraseña" error={errorDe('confirmacion')}>
        <input
          id="confirmacion"
          type={tipo}
          autoComplete="new-password"
          maxLength={72}
          value={confirmacion}
          onChange={(e) => cambio.cambiar('confirmacion', e.target.value)}
          aria-invalid={Boolean(errorDe('confirmacion'))}
          aria-describedby={describir('confirmacion')}
        />
      </Campo>

      <ul id="requisitos" className="cambio-contrasena__requisitos" aria-label="Requisitos de la nueva contraseña">
        {requisitos.map(({ texto, cumple }) => (
          <li key={texto} className={cumple ? 'cumple' : undefined}>
            {cumple ? <Check size={16} aria-hidden="true" /> : <Circle size={16} aria-hidden="true" />}
            {texto}
            <span className="solo-lectores">{cumple ? ': cumplido' : ': pendiente'}</span>
          </li>
        ))}
      </ul>

      <label className="cambio-contrasena__ver">
        <input type="checkbox" checked={ver} onChange={(e) => setVer(e.target.checked)} />
        {ver ? <EyeOff size={16} aria-hidden="true" /> : <Eye size={16} aria-hidden="true" />}
        Mostrar contraseñas
      </label>

      <div className="cambio-contrasena__botones">
        <Boton type="submit" bloque disabled={enviando} aria-busy={enviando}>
          {enviando ? 'Guardando…' : 'Guardar contraseña'}
        </Boton>
        {acciones}
      </div>
    </form>
  )
}

function Campo({ id, etiqueta, error, children }) {
  return (
    <div className="cambio-contrasena__campo">
      <label htmlFor={id}>{etiqueta}</label>
      {children}
      {error && (
        <span id={`${id}-error`} className="cambio-contrasena__error-campo">
          {error}
        </span>
      )}
    </div>
  )
}
