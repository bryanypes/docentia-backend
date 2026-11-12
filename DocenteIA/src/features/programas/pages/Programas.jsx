import { BookOpen, Pencil, RefreshCw, Trash2 } from 'lucide-react'
import { useCallback, useState } from 'react'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { Aviso } from '../../../shared/components/ui/Aviso/Aviso.jsx'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Campo } from '../../../shared/components/ui/Campo/Campo.jsx'
import { ErrorApi } from '../../../shared/components/ui/ErrorApi/ErrorApi.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Panel } from '../../../shared/components/ui/Panel/Panel.jsx'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { listarFacultades } from '../../facultades/services/facultadesService.js'
import {
  actualizarPrograma,
  crearPrograma,
  eliminarPrograma,
  listarProgramas,
} from '../services/programasService.js'

const VACIO = { nombre: '', idFacultad: '' }

export default function Programas() {
  const [filtro, setFiltro] = useState('')
  const cargar = useCallback(() => listarProgramas(filtro || null), [filtro])
  const programas = useFetch(cargar, [cargar])
  const facultades = useFetch(listarFacultades)
  const [enEdicion, setEnEdicion] = useState(null)
  const [formulario, setFormulario] = useState(VACIO)
  const [error, setError] = useState(null)
  const [resultado, setResultado] = useState(null)

  function editar(programa) {
    setEnEdicion(programa)
    setFormulario(programa ? { nombre: programa.nombre, idFacultad: String(programa.idFacultad) } : VACIO)
    setError(null)
  }

  async function guardar(evento) {
    evento.preventDefault()
    setError(null)
    setResultado(null)
    try {
      const datos = { nombre: formulario.nombre.trim(), idFacultad: Number(formulario.idFacultad) }
      await (enEdicion ? actualizarPrograma(enEdicion.idPrograma, datos) : crearPrograma(datos))
      setResultado(`Programa ${datos.nombre} ${enEdicion ? 'actualizado' : 'creado'}.`)
      editar(null)
      programas.recargar()
    } catch (errorApi) {
      setError(errorApi)
    }
  }

  async function eliminar(programa) {
    if (!window.confirm(`¿Eliminar el programa «${programa.nombre}»?`)) return
    setError(null)
    setResultado(null)
    try {
      await eliminarPrograma(programa.idPrograma)
      setResultado(`Programa ${programa.nombre} eliminado.`)
      programas.recargar()
    } catch (errorApi) {
      setError(errorApi)
    }
  }

  const lista = programas.datos ?? []
  const opciones = (facultades.datos ?? []).map((facultad) => (
    <option key={facultad.idFacultad} value={facultad.idFacultad}>
      {facultad.nombre}
    </option>
  ))

  return (
    <>
      <Encabezado miga="Vicerrectoría · Programas" titulo="Programas" subtitulo="Programas académicos de cada facultad." />
      <div className="pagina">
        {resultado && <Aviso tipo="exito" titulo={resultado} />}
        <ErrorApi error={error} nombresCampos={{ nombre: 'Nombre', idFacultad: 'Facultad' }} />
        <Panel titulo={enEdicion ? `Editar ${enEdicion.nombre}` : 'Nuevo programa'} icono={BookOpen}>
          <form className="formulario-simple" onSubmit={guardar}>
            <Campo id="nombrePrograma" etiqueta="Nombre">
              <input
                id="nombrePrograma"
                className="campo__control"
                value={formulario.nombre}
                maxLength={120}
                onChange={(e) => setFormulario((f) => ({ ...f, nombre: e.target.value }))}
                required
              />
            </Campo>
            <Campo id="facultadPrograma" etiqueta="Facultad">
              <select
                id="facultadPrograma"
                className="campo__control"
                value={formulario.idFacultad}
                onChange={(e) => setFormulario((f) => ({ ...f, idFacultad: e.target.value }))}
                required
              >
                <option value="">Elige una facultad</option>
                {opciones}
              </select>
            </Campo>
            <Boton type="submit">{enEdicion ? 'Guardar cambios' : 'Crear programa'}</Boton>
            {enEdicion && (
              <Boton variante="contorno" onClick={() => editar(null)}>
                Cancelar
              </Boton>
            )}
          </form>
        </Panel>

        <Campo id="filtroFacultadProgramas" etiqueta="Filtrar por facultad">
          <select
            id="filtroFacultadProgramas"
            className="campo__control"
            value={filtro}
            onChange={(e) => setFiltro(e.target.value)}
          >
            <option value="">Todas</option>
            {opciones}
          </select>
        </Campo>

        {programas.cargando && !programas.datos && <Cargando texto="Cargando programas" bloques={1} />}
        {programas.error && (
          <EstadoVista tipo="error" titulo="No fue posible cargar los programas" mensaje={programas.error.message}>
            <Boton icono={RefreshCw} onClick={programas.recargar}>Intentar de nuevo</Boton>
          </EstadoVista>
        )}
        {!programas.cargando && !programas.error && lista.length === 0 && (
          <EstadoVista titulo="Sin programas" mensaje="No hay programas para este filtro." />
        )}
        {lista.length > 0 && (
          <div className="tabla-contenedor">
            <table className="tabla">
              <thead>
                <tr>
                  <th scope="col">Programa</th>
                  <th scope="col">Facultad</th>
                  <th scope="col">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {lista.map((programa) => (
                  <tr key={programa.idPrograma}>
                    <td className="tabla__fuerte">{programa.nombre}</td>
                    <td>{programa.nombreFacultad}</td>
                    <td>
                      <div className="acciones-fila">
                        <Boton
                          variante="enlace"
                          icono={Pencil}
                          aria-label={`Editar ${programa.nombre}`}
                          onClick={() => editar(programa)}
                        >
                          Editar
                        </Boton>
                        <Boton
                          variante="enlace"
                          icono={Trash2}
                          aria-label={`Eliminar ${programa.nombre}`}
                          onClick={() => eliminar(programa)}
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
