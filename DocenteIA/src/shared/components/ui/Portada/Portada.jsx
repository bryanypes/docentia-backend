import './Portada.css'

/**
 * Portada de un Inicio: franja azul de la marca con un rótulo pequeño, el título grande, un detalle
 * y, abajo, lo que cada rol necesita (filtros, cifras o avance). `robot` agrega la ilustración de
 * DocenteIA a la derecha.
 */
export function Portada({ id, icono: Icono, rotulo, titulo, detalle, robot, children }) {
  return (
    <section className={`portada${robot ? ' portada--con-robot' : ''}`} aria-labelledby={id}>
      <div className="portada__texto">
        {rotulo && (
          <span className="portada__rotulo">
            {Icono && <Icono size={14} aria-hidden="true" />}
            {rotulo}
          </span>
        )}
        <h2 id={id} className="portada__titulo">
          {titulo}
        </h2>
        {detalle && <span className="portada__detalle">{detalle}</span>}
      </div>
      {robot && <img className="portada__robot" src={robot} alt="" aria-hidden="true" />}
      {children && <div className="portada__pie">{children}</div>}
    </section>
  )
}

/** Cifra corta sobre la portada: rótulo, valor y una nota. */
export function CifraPortada({ rotulo, valor, nota }) {
  return (
    <div className="portada__cifra">
      <span className="portada__cifra-rotulo">{rotulo}</span>
      <strong className="portada__cifra-valor">{valor}</strong>
      {nota && <span className="portada__cifra-nota">{nota}</span>}
    </div>
  )
}
