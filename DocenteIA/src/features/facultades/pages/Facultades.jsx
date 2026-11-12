import { Building2, Pencil, RefreshCw, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import {
  actualizarFacultad,
  crearFacultad,
  eliminarFacultad,
  listarFacultades,
} from '../services/facultadesService.js'

export default function Facultades() {
  const facultades = useFetch(listarFacultades)
  const [enEdicion, setEnEdicion] = useState(null)
  const [nombre, setNombre] = useState('')
  const [error, setError] = useState(null)
  const [resultado, setResultado] = useState(null)

  function editar(facultad) {
    setEnEdicion(facultad)
    setNombre(facultad?.nombre ?? '')
    setError(null)
  }

  async function guardar(evento) {
    evento.preventDefault()
    setError(null)
    setResultado(null)
    try {
      const datos = { nombre: nombre.trim() }
      await (enEdicion ? actualizarFacultad(enEdicion.idFacultad, datos) : crearFacultad(datos))
      setResultado(`Facultad ${datos.nombre} ${enEdicion ? 'actualizada' : 'creada'}.`)
      editar(null)
      facultades.recargar()
    } catch (errorApi) {
      setError(errorApi)
    }
  }

  async function eliminar(facultad) {
    if (!window.confirm(`¿Eliminar la facultad «${facultad.nombre}»?`)) return
    setError(null)
    setResultado(null)
    try {
      await eliminarFacultad(facultad.idFacultad)
      setResultado(`Facultad ${facultad.nombre} eliminada.`)
      facultades.recargar()
    } catch (errorApi) {
      setError(errorApi)
    }
  }

  const lista = facultades.datos ?? []

  return (
    <>
      <Encabezado miga="Vicerrectoría · Facultades" titulo="Facultades" subtitulo="Facultades de la universidad." />
      <div className="pagina">
        {resultado && <Aviso tipo="exito" titulo={resultado} />}
        <ErrorApi error={error} nombresCampos={{ nombre: 'Nombre' }} />
        <Panel titulo={enEdicion ? `Editar ${enEdicion.nombre}` : 'Nueva facultad'} icono={Building2}>
          <form className="formulario-simple" onSubmit={guardar}>
            <Campo id="nombreFacultad" etiqueta="Nombre">
              <input
                id="nombreFacultad"
                className="campo__control"
                value={nombre}
                maxLength={120}
                onChange={(e) => setNombre(e.target.value)}
                required
              />
            </Campo>
            <Boton type="submit">{enEdicion ? 'Guardar cambios' : 'Crear facultad'}</Boton>
            {enEdicion && (
              <Boton variante="contorno" onClick={() => editar(null)}>
                Cancelar
              </Boton>
            )}
          </form>
        </Panel>

        {facultades.cargando && !facultades.datos && <Cargando texto="Cargando facultades" bloques={1} />}
        {facultades.error && (
          <EstadoVista tipo="error" titulo="No fue posible cargar las facultades" mensaje={facultades.error.message}>
            <Boton icono={RefreshCw} onClick={facultades.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}
        {!facultades.cargando && !facultades.error && lista.length === 0 && (
          <EstadoVista titulo="Aún no hay facultades" mensaje="Crea la primera con el formulario de arriba." />
        )}
        {lista.length > 0 && (
          <div className="tabla-contenedor">
            <table className="tabla">
              <thead>
                <tr>
                  <th scope="col">Facultad</th>
                  <th scope="col">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {lista.map((facultad) => (
                  <tr key={facultad.idFacultad}>
                    <td className="tabla__fuerte">{facultad.nombre}</td>
                    <td>
                      <div className="acciones-fila">
                        <Boton
                          variante="enlace"
                          icono={Pencil}
                          aria-label={`Editar ${facultad.nombre}`}
                          onClick={() => editar(facultad)}
                        >
                          Editar
                        </Boton>
                        <Boton
                          variante="enlace"
                          icono={Trash2}
                          aria-label={`Eliminar ${facultad.nombre}`}
                          onClick={() => eliminar(facultad)}
                        >
                          Eliminar
                        </Boton>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  )
}
