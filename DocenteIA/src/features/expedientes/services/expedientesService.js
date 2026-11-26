import { api } from '../../../services/api.js'

/** Expediente del docente autenticado, sin notas individuales de coordinación ni vicerrectoría. */
export function obtenerExpedientePropio() {
  return api.get('/expedientes/propio')
}

/** Expediente completo de un docente, para coordinación y vicerrectoría. */
export function obtenerExpedienteDocente(idDocente) {
  return api.get(`/expedientes/${encodeURIComponent(idDocente)}`)
}

export function obtenerComparativoDocente(idDocente) {
  return api.get(`/expedientes/${encodeURIComponent(idDocente)}/comparativo`)
}

export function obtenerComparativoPropio() {
  return api.get('/expedientes/propio/comparativo')
}

/** Reporte PDF colectivo del periodo activo, limitado al alcance del coordinador. */
export function descargarReporteEvaluacionPdf() {
  return api.get('/expedientes/reporte/pdf', { binario: true })
}
