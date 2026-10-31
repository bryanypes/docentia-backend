import './Panel.css'

/** Tarjeta del maquetado. `icono` (de lucide-react) aparece como insignia junto al título. */
export function Panel({ titulo, descripcion, icono: Icono, acciones, children, className = '', ...props }) {
  return (
    <section className={`panel ${className}`} {...props}>
      {(titulo || acciones) && (
        <div className="panel__cabecera">
          <div className="panel__encabezado">
            {Icono && (
              <span className="panel__icono" aria-hidden="true">
                <Icono size={19} />
              </span>
            )}
            <div>
              {titulo && <h2 className="panel__titulo">{titulo}</h2>}
              {descripcion && <p className="panel__descripcion">{descripcion}</p>}
            </div>
          </div>
          {acciones}
        </div>
      )}
      {children}
    </section>
  )
}
