/** Etiqueta, control, ayuda y error de un campo de formulario. El control lleva el mismo `id`. */
export function Campo({ id, etiqueta, ayuda, error, children }) {
  return (
    <div className="campo">
      <label htmlFor={id} className="rotulo rotulo--campo">
        {etiqueta}
      </label>
      {children}
      {ayuda && (
        <span id={`${id}-ayuda`} className="campo__ayuda">
          {ayuda}
        </span>
      )}
      {error && (
        <span id={`${id}-error`} className="campo__error">
          {error}
        </span>
      )}
    </div>
  )
}
