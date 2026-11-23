import { api } from '../../../services/api.js'

/** Actas de un docente, de la versión más nueva a la más antigua. El docente recibe las suyas. */
export function listarActas(idDocente) {
  return api.get(idDocente ? `/actas?idDocente=${encodeURIComponent(idDocente)}` : '/actas')
}

/** `tipo`: COMPROMISO o RETROALIMENTACION; sin él, el que corresponde a la nota. */
export function crearActa(idDocente, tipo) {
  return api.post('/actas', tipo ? { idDocente: Number(idDocente), tipo } : { idDocente: Number(idDocente) })
}

/** Cambia el tipo del acta antes de la primera firma. */
export function cambiarTipoActa(idActa, tipo) {
  return api.patch(`/actas/${encodeURIComponent(idActa)}/tipo`, { tipo })
}

/** Lo que se habló en la reunión; vacío lo quita. Solo antes de la primera firma. */
export function guardarObservacionesActa(idActa, texto) {
  return api.put(`/actas/${encodeURIComponent(idActa)}/observaciones`, { texto: texto || null })
}

export function agregarCompromiso(idActa, compromiso) {
  return api.post(`/actas/${encodeURIComponent(idActa)}/compromisos`, compromiso)
}

export function quitarCompromiso(idActa, idCompromiso) {
  return api.delete(`/actas/${encodeURIComponent(idActa)}/compromisos/${encodeURIComponent(idCompromiso)}`)
}

/**
 * La firma es irreversible: el backend exige `confirmacion` en true y el trazo dibujado en la
 * página (PNG como data URL), que se imprime en el PDF.
 */
export function firmarActa(idActa, trazo) {
  return api.post(`/actas/${encodeURIComponent(idActa)}/firmas`, { confirmacion: true, trazo })
}

export function corregirActa(idActa, motivo) {
  return api.post(`/actas/${encodeURIComponent(idActa)}/correcciones`, { motivo })
}

export function descargarPdfActa(idActa) {
  return api.get(`/actas/${encodeURIComponent(idActa)}/pdf`, { binario: true })
}

/** Todas las actas firmadas, para la administración y la vicerrectoría; sin notas. */
export function listarRegistroActas() {
  return api.get('/actas/registro')
}

/** Búsqueda por código para la administración y la vicerrectoría; sin notas. */
export function buscarActaPorCodigo(codigo) {
  return api.get(`/actas/codigo/${encodeURIComponent(codigo)}`)
}

/** Consulta pública por código; no necesita sesión. */
export function verificarActa(codigo) {
  return api.get(`/verificacion-actas/${encodeURIComponent(codigo)}`, { publica: true })
}

export function obtenerPlanDocente(idDocente) {
  return api.get(`/docentes/${encodeURIComponent(idDocente)}/plan-seguimiento`)
}

export function obtenerPlanPropio() {
  return api.get('/plan-seguimiento/propio')
}

export function marcarCumplido(idCompromiso, observacion) {
  return api.put(`/compromisos/${encodeURIComponent(idCompromiso)}/cumplimiento`, { observacion })
}

/** Acciones sugeridas del periodo; sin periodo, las del activo. */
export function listarAccionesSugeridas(periodo) {
  return api.get(`/acciones-sugeridas${periodo ? `?periodo=${encodeURIComponent(periodo)}` : ''}`)
}
