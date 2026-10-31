import { api, ApiError } from '../../../services/api.js'

/** multipart/form-data con el archivo y, si llegan, el mapeo (columna → campo) o el nombre de una plantilla. */
function formularioCsv(archivo, { mapeo, plantilla } = {}) {
  const datos = new FormData()
  datos.append('archivo', archivo)
  if (mapeo) datos.append('mapeo', JSON.stringify(mapeo))
  else if (plantilla) datos.append('plantilla', plantilla)
  return datos
}

/** Valida el CSV sin guardar nada. Responde el reporte aunque el archivo tenga errores (mirar `valido`). */
export function validarCsv(archivo, opciones) {
  return api.post('/importaciones/csv/validar', formularioCsv(archivo, opciones))
}

/**
 * Importa el CSV en el periodo activo. Devuelve { importacion, reporte } tanto si se importó como si
 * se rechazó por errores (422, no se guardó nada): `importacion.resultado` dice cuál fue.
 */
export async function importarCsv(archivo, opciones) {
  try {
    return await api.post('/importaciones/csv', formularioCsv(archivo, opciones))
  } catch (error) {
    if (error instanceof ApiError && error.status === 422 && error.datos?.reporte) return error.datos
    throw error
  }
}

/** Cargas registradas (también las rechazadas). Coordinación ve las suyas; vicerrectoría todas. */
export function listarImportaciones(idPeriodo) {
  const consulta = idPeriodo ? `?idPeriodo=${encodeURIComponent(idPeriodo)}` : ''
  return api.get(`/importaciones${consulta}`)
}

/** Plantillas de mapeo del coordinador autenticado. */
export function listarPlantillas() {
  return api.get('/importaciones/plantillas')
}

/** Crea la plantilla o reemplaza la que tenga el mismo nombre. */
export function guardarPlantilla(plantilla) {
  return api.post('/importaciones/plantillas', plantilla)
}

export function eliminarPlantilla(idPlantilla) {
  return api.delete(`/importaciones/plantillas/${encodeURIComponent(idPlantilla)}`)
}
