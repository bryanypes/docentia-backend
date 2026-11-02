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

export function agregarCompromiso(idActa, compromiso) {
  return api.post(`/actas/${encodeURIComponent(idActa)}/compromisos`, compromiso)
}

export function quitarCompromiso(idActa, idCompromiso) {
  return api.delete(`/actas/${encodeURIComponent(idActa)}/compromisos/${encodeURIComponent(idCompromiso)}`)
}

/** La firma es irreversible: el backend exige `confirmacion` en true. */
export function firmarActa(idActa) {
  return api.post(`/actas/${encodeURIComponent(idActa)}/firmas`, { confirmacion: true })
}

export function corregirActa(idActa, motivo) {
  return api.post(`/actas/${encodeURIComponent(idActa)}/correcciones`, { motivo })
}

export function descargarPdfActa(idActa) {
  return api.get(`/actas/${encodeURIComponent(idActa)}/pdf`, { binario: true })
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
