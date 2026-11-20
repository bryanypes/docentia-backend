import { Search } from 'lucide-react'
import { useState } from 'react'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'

/** Campo para escribir el código impreso en el PDF del acta (DOC-XXXXXXXXXXXX). */
export function FormularioCodigo({ inicial = '', alBuscar, etiqueta = 'Verificar' }) {
  const [codigo, setCodigo] = useState(inicial)
  const limpio = codigo.trim().toUpperCase()

  return (
    <form
      className="codigo-acta"
      role="search"
      onSubmit={(e) => {
        e.preventDefault()
        if (limpio) alBuscar(limpio)
      }}
    >
      <label htmlFor="codigo-acta" className="rotulo rotulo--campo">
        Código del acta
      </label>
      <div className="codigo-acta__fila">
        <input
          id="codigo-acta"
          className="campo__control codigo-acta__control"
          value={codigo}
          placeholder="DOC-1A2B3C4D5E6F"
          maxLength={40}
          autoComplete="off"
          spellCheck={false}
          onChange={(e) => setCodigo(e.target.value)}
        />
        <Boton type="submit" icono={Search} disabled={!limpio}>
          {etiqueta}
        </Boton>
      </div>
    </form>
  )
}
