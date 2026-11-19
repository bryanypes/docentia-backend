import { api } from '../../services/api.js'

/** Pide corregir un formulario ya enviado: { tipo: 'AUTO' | 'COEVAL', idDocente?, motivo }. */
export function solicitarCorreccion(pedido) {
  return api.post('/solicitudes-reapertura', pedido)
}

export function listarMisSolicitudes() {
  return api.get('/solicitudes-reapertura/mias')
}

/** Las que verifica la coordinación (su facultad) o la vicerrectoría. */
export function listarPorVerificar() {
  return api.get('/solicitudes-reapertura/por-verificar')
}

/** Todas, para la administración. */
export function listarTodasLasSolicitudes() {
  return api.get('/solicitudes-reapertura')
}

export function verificarSolicitud(idSolicitud, aprobada, respuesta) {
  return api.post(`/solicitudes-reapertura/${encodeURIComponent(idSolicitud)}/verificacion`, {
    aprobada,
    respuesta: respuesta || null,
  })
}

/** La administración reabre el formulario (aprobada) o rechaza la solicitud. */
export function atenderSolicitud(idSolicitud, aprobada, respuesta) {
  return api.post(`/solicitudes-reapertura/${encodeURIComponent(idSolicitud)}/atencion`, {
    aprobada,
    respuesta: respuesta || null,
  })
}
